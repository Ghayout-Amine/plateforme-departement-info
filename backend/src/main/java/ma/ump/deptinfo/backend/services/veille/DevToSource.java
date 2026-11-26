package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * PGDI2026-88 : API publique de Dev.to (sans clé), filtrée pour écarter les billets hors-sujet.
 * GET https://dev.to/api/articles?per_page=15&top=7[&tag=computerscience]
 */
public class DevToSource implements TechFeedSource {

    // Tags grand public / humeur / mèmes exclus pour ne garder que du contenu informatique et scientifique
    private static final Set<String> EXCLUDED_TAGS = Set.of(
            "watercooler", "memes", "meme", "humor", "fun", "funny",
            "discuss", "mentalhealth", "career", "writing", "productivity",
            "motivation", "community", "podcast", "codenewbie", "codenewbiestory",
            "jokes", "shitpost", "story", "entertainment"
    );

    private final String apiUrl;
    private final String tag;
    private final RestClient http;

    public DevToSource(String apiUrl, String tag, int timeoutSeconds) {
        this.apiUrl = apiUrl;
        this.tag = tag == null ? "" : tag.trim();
        this.http = FeedHttp.createClient(timeoutSeconds);
    }

    @Override
    public String name() {
        return "Dev.to";
    }

    @Override
    public List<TechArticleDto> fetch() {
        String url = apiUrl + "?per_page=15&top=7" + (tag.isEmpty() ? "" : "&tag=" + tag);
        List<Map<String, Object>> response = FeedHttp.getWithRetry(() -> http.get()
                .uri(url)
                .header("User-Agent", "PGDI2026-Veille/1.0")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { }));
        return parse(response);
    }

    /** Transforme la réponse JSON de Dev.to en articles informatiques en filtrant les tags hors-sujet. */
    static List<TechArticleDto> parse(List<Map<String, Object>> response) {
        List<TechArticleDto> articles = new ArrayList<>();
        if (response == null) return articles;
        for (Map<String, Object> item : response) {
            if (hasExcludedTag(item)) continue;
            String title = FeedParser.clean(str(item, "title"));
            String link = str(item, "url");
            if (title.isEmpty() || !FeedParser.isHttpLink(link)) continue;
            String image = str(item, "cover_image");
            if (!FeedParser.isHttpLink(image)) image = str(item, "social_image");
            articles.add(new TechArticleDto(
                    title,
                    FeedParser.summary(str(item, "description")),
                    link,
                    FeedParser.isHttpLink(image) ? image : null,
                    FeedParser.parseDate(str(item, "published_at")),
                    "Dev.to"));
        }
        return articles;
    }

    private static boolean hasExcludedTag(Map<String, Object> item) {
        Object tagList = item.get("tag_list");
        if (tagList instanceof Collection<?> list) {
            for (Object tag : list) {
                if (tag != null && EXCLUDED_TAGS.contains(tag.toString().trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
        String tags = str(item, "tags");
        if (tags.isEmpty()) return false;
        for (String tag : tags.split(",")) {
            if (EXCLUDED_TAGS.contains(tag.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString().trim();
    }
}
