package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PGDI2026-88 : lecture d'un flux RSS 2.0 ou Atom avec le parseur XML du JDK (aucune dépendance).
 * Pour chaque article : titre, résumé (texte brut), date de publication, lien source et image.
 * Le parseur refuse les DOCTYPE et les entités externes (protection contre les attaques XXE).
 */
public final class FeedParser {

    static final int SUMMARY_MAX = 280;
    private static final Pattern IMG_SRC = Pattern.compile("<img[^>]+src=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAGS = Pattern.compile("<[^>]*>");

    private FeedParser() {
    }

    public static List<TechArticleDto> parse(String xml, String sourceName) {
        Document doc = toDocument(xml);
        Element root = doc.getDocumentElement();
        boolean atom = "feed".equals(localName(root));
        List<TechArticleDto> articles = new ArrayList<>();
        for (Element item : descendants(root, atom ? "entry" : "item")) {
            TechArticleDto article = atom ? fromAtom(item, sourceName) : fromRss(item, sourceName);
            if (article != null) articles.add(article);
        }
        return articles;
    }

    // ---------- RSS 2.0 ----------

    private static TechArticleDto fromRss(Element item, String source) {
        String title = clean(text(item, "title"));
        String link = rssLink(item);
        if (title.isEmpty() || !isHttpLink(link)) return null;

        String description = text(item, "description");
        String fullContent = text(item, "encoded");          // content:encoded
        String date = firstNonEmpty(text(item, "pubDate"), text(item, "date"));   // dc:date

        return new TechArticleDto(title, summary(description.isEmpty() ? fullContent : description), link,
                rssImage(item, description + fullContent), parseDate(date), source);
    }

    /** <link> texte d'un item RSS ; certains flux ajoutent un <atom:link href="..."/> qu'on ignore s'il est vide. */
    private static String rssLink(Element item) {
        for (Element e : children(item)) {
            if (!"link".equals(localName(e))) continue;
            String value = e.getTextContent() == null ? "" : e.getTextContent().trim();
            if (value.isEmpty()) value = e.getAttribute("href").trim();
            if (isHttpLink(value)) return value;
        }
        return "";
    }

    private static String rssImage(Element item, String html) {
        for (Element e : children(item)) {
            String name = localName(e);
            String url = e.getAttribute("url");
            if ("enclosure".equals(name) && e.getAttribute("type").startsWith("image") && isHttpLink(url)) return url;
            if (("content".equals(name) || "thumbnail".equals(name)) && isHttpLink(url)) return url;   // media:content / media:thumbnail
        }
        return imageFromHtml(html);
    }

    // ---------- Atom ----------

    private static TechArticleDto fromAtom(Element entry, String source) {
        String title = clean(text(entry, "title"));
        String link = atomLink(entry);
        if (title.isEmpty() || !isHttpLink(link)) return null;

        String summary = text(entry, "summary");
        String content = text(entry, "content");
        String date = firstNonEmpty(text(entry, "published"), text(entry, "updated"));

        return new TechArticleDto(title, summary(summary.isEmpty() ? content : summary), link,
                imageFromHtml(summary + content), parseDate(date), source);
    }

    private static String atomLink(Element entry) {
        String fallback = "";
        for (Element e : children(entry)) {
            if (!"link".equals(localName(e))) continue;
            String rel = e.getAttribute("rel");
            if (rel.isEmpty() || "alternate".equals(rel)) return e.getAttribute("href").trim();
            if (fallback.isEmpty()) fallback = e.getAttribute("href").trim();
        }
        return fallback;
    }

    // ---------- outils communs ----------

    /** Retire le HTML, décode les entités courantes, compacte les espaces et coupe à 280 caractères. */
    static String summary(String html) {
        String text = clean(TAGS.matcher(html == null ? "" : html).replaceAll(" "));
        return text.length() <= SUMMARY_MAX ? text : text.substring(0, SUMMARY_MAX - 1).trim() + "…";
    }

    static String clean(String value) {
        if (value == null) return "";
        return value.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&#039;", "'").replace("&apos;", "'")
                .replaceAll("\\s+", " ").trim();
    }

    /** Dates RSS (RFC 1123, ex. « Tue, 24 Nov 2026 10:00:00 GMT ») ou ISO 8601 (Atom). Null si illisible. */
    static OffsetDateTime parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        try {
            return OffsetDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH));
        } catch (DateTimeParseException ignored) {
            // pas au format RFC 1123 : on essaie ISO 8601
        }
        try {
            return OffsetDateTime.parse(v);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static boolean isHttpLink(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://"));
    }

    private static String imageFromHtml(String html) {
        if (html == null) return null;
        Matcher m = IMG_SRC.matcher(html);
        return m.find() && isHttpLink(m.group(1)) ? m.group(1) : null;
    }

    private static String firstNonEmpty(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static Document toDocument(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());   // pas de « [Fatal Error] » dans la console : l'erreur est relancée
            return builder.parse(new InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new IllegalStateException("Flux XML illisible : " + e.getMessage(), e);
        }
    }

    /** Texte du premier enfant direct portant ce nom local (sans préfixe de namespace). */
    private static String text(Element parent, String name) {
        for (Element e : children(parent)) {
            if (name.equals(localName(e))) return e.getTextContent() == null ? "" : e.getTextContent();
        }
        return "";
    }

    private static List<Element> children(Element parent) {
        List<Element> result = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element e) result.add(e);
        }
        return result;
    }

    private static List<Element> descendants(Element root, String name) {
        List<Element> result = new ArrayList<>();
        NodeList all = root.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node node = all.item(i);
            if (node instanceof Element e && name.equals(localName(e))) result.add(e);
        }
        return result;
    }

    private static String localName(Element e) {
        return e.getLocalName() != null ? e.getLocalName() : e.getTagName();
    }
}
