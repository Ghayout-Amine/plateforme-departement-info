package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FeedParserTest {

    private static final String RSS = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0" xmlns:media="http://search.yahoo.com/mrss/" xmlns:atom="http://www.w3.org/2005/Atom">
              <channel>
                <title>Le Monde Informatique</title>
                <atom:link href="https://www.lemondeinformatique.fr/rss.xml" rel="self"/>
                <item>
                  <title>Mistral AI s&apos;allie à NumSpot</title>
                  <link>https://www.lemondeinformatique.fr/actualites/lire-mistral-1.html</link>
                  <description><![CDATA[<p>Une IA <b>hébergée</b> en France.</p><img src="https://img.lmi.fr/mistral.jpg"/>]]></description>
                  <pubDate>Tue, 24 Nov 2026 10:00:00 GMT</pubDate>
                </item>
                <item>
                  <title>Article avec image en enclosure</title>
                  <link>https://www.lemondeinformatique.fr/actualites/lire-2.html</link>
                  <description>Texte simple</description>
                  <enclosure url="https://img.lmi.fr/2.jpg" type="image/jpeg" length="1000"/>
                  <pubDate>Mon, 23 Nov 2026 08:30:00 +0100</pubDate>
                </item>
                <item>
                  <title>Sans lien valide</title>
                  <link>javascript:alert(1)</link>
                </item>
              </channel>
            </rss>
            """;

    private static final String ATOM = """
            <?xml version="1.0" encoding="utf-8"?>
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Blog tech</title>
              <entry>
                <title>Angular 22 : les nouveautés</title>
                <link rel="alternate" href="https://blog.example.org/angular-22"/>
                <published>2026-11-20T14:00:00Z</published>
                <summary>Les signaux deviennent la norme.</summary>
              </entry>
            </feed>
            """;

    @Test
    @DisplayName("RSS 2.0 : titre, lien, résumé sans HTML, date RFC 1123 et image")
    void parseRss() {
        List<TechArticleDto> articles = FeedParser.parse(RSS, "Le Monde Informatique");

        assertEquals(2, articles.size(), "l'item sans lien http(s) est ignoré");
        TechArticleDto first = articles.get(0);
        assertEquals("Mistral AI s'allie à NumSpot", first.getTitle());
        assertEquals("https://www.lemondeinformatique.fr/actualites/lire-mistral-1.html", first.getLink());
        assertEquals("Une IA hébergée en France.", first.getSummary());
        assertEquals("https://img.lmi.fr/mistral.jpg", first.getImageUrl());
        assertEquals(OffsetDateTime.of(2026, 11, 24, 10, 0, 0, 0, ZoneOffset.UTC), first.getPublishedAt());
        assertEquals("Le Monde Informatique", first.getSource());

        assertEquals("https://img.lmi.fr/2.jpg", articles.get(1).getImageUrl());
        assertEquals(ZoneOffset.ofHours(1), articles.get(1).getPublishedAt().getOffset());
    }

    @Test
    @DisplayName("Atom : lien alternate, date ISO 8601 et résumé")
    void parseAtom() {
        List<TechArticleDto> articles = FeedParser.parse(ATOM, "Blog");

        assertEquals(1, articles.size());
        assertEquals("https://blog.example.org/angular-22", articles.get(0).getLink());
        assertEquals("Les signaux deviennent la norme.", articles.get(0).getSummary());
        assertNotNull(articles.get(0).getPublishedAt());
        assertNull(articles.get(0).getImageUrl());
    }

    @Test
    @DisplayName("Sécurité : un flux avec DOCTYPE (attaque XXE) est refusé")
    void rejectsDoctype() {
        String xxe = """
                <?xml version="1.0"?>
                <!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <rss><channel><item><title>&xxe;</title><link>https://a.b/c</link></item></channel></rss>
                """;

        assertThrows(IllegalStateException.class, () -> FeedParser.parse(xxe, "Pirate"));
    }

    @Test
    @DisplayName("Flux invalide : exception (la source sera signalée indisponible)")
    void invalidXml() {
        assertThrows(IllegalStateException.class, () -> FeedParser.parse("<html><body>503", "Source"));
    }

    @Test
    @DisplayName("Résumé limité à 280 caractères")
    void summaryIsTruncated() {
        String summary = FeedParser.summary("a ".repeat(400));

        assertTrue(summary.length() <= FeedParser.SUMMARY_MAX);
        assertTrue(summary.endsWith("…"));
    }

    @Test
    @DisplayName("Dev.to : JSON de l'API transformé en articles, image de couverture ou sociale")
    void parseDevTo() {
        List<Map<String, Object>> json = List.of(
                Map.<String, Object>of("title", "Spring Boot 4 en pratique",
                        "url", "https://dev.to/alice/spring-boot-4",
                        "description", "Tour des nouveautés",
                        "social_image", "https://media.dev.to/social.png",
                        "published_at", "2026-11-22T09:15:00Z"),
                Map.<String, Object>of("title", "Sans URL", "description", "ignoré"));

        List<TechArticleDto> articles = DevToSource.parse(json);

        assertEquals(1, articles.size());
        assertEquals("Dev.to", articles.get(0).getSource());
        assertEquals("https://media.dev.to/social.png", articles.get(0).getImageUrl());
        assertNotNull(articles.get(0).getPublishedAt());
    }
}
