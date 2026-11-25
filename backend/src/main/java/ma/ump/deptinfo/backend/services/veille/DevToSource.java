package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PGDI2026-88 : API publique de Dev.to (sans clé), articles les plus lus des 7 derniers jours.
 * GET https://dev.to/api/articles?per_page=15&top=7[&tag=java]
 */
public class DevToSource implements TechFeedSource {

    private final String apiUrl;
    private final String tag;
    private final RestClient http;

    public DevToSource(String apiUrl, String tag, int timeoutSeconds) {
        this.apiUrl = apiUrl;
        this.tag = tag == null ? "" : tag.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String name() {
        return "Dev.to";
    }

    @Override
    public List<TechArticleDto> fetch() {
        String url = apiUrl + "?per_page=15&top=7" + (tag.isEmpty() ? "" : "&tag=" + tag);
        List<Map<String, Object>> response = http.get()
                .uri(url)
                .header("User-Agent", "PGDI2026-Veille/1.0")
                .retrieve()
                .body(new ParameterizedTypeReference<List<Map<String, Object>>>() { });
        return parse(response);
    }

    /** Transforme la réponse JSON de Dev.to (liste d'articles) en articles de veille. */
    static List<TechArticleDto> parse(List<Map<String, Object>> response) {
        List<TechArticleDto> articles = new ArrayList<>();
        if (response == null) return articles;
        for (Map<String, Object> item : response) {
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

    private static String str(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString().trim();
    }
}
