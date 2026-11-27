package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * PGDI2026-88 : un flux RSS ou Atom (arXiv, The Hacker News...), avec timeout et retry transitoire.
 * Les flux sont déclarés dans app.veille.rss-feeds (voir {@link TechWatchService}).
 */
public class RssFeedSource implements TechFeedSource {

    private static final int MAX_ITEMS_PER_FEED = 15;

    private final String name;
    private final String url;
    private final RestClient http;

    public RssFeedSource(String name, String url, int timeoutSeconds) {
        this.name = name;
        this.url = url;
        this.http = FeedHttp.createClient(timeoutSeconds);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<TechArticleDto> fetch() {
        String xml = FeedHttp.getWithRetry(() -> http.get()
                .uri(url)
                .header("User-Agent", "PGDI2026-Veille/1.0")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .retrieve()
                .body(String.class));
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("Flux vide");
        }
        List<TechArticleDto> parsed = FeedParser.parse(xml, name);
        return TechWatchService.deduplicateAndSort(parsed).stream()
                .limit(MAX_ITEMS_PER_FEED)
                .toList();
    }
}
