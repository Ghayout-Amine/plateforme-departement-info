package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * PGDI2026-88 : un flux RSS ou Atom (ex. Le Monde Informatique), lu avec un délai maximal (timeout).
 * Les flux sont déclarés dans app.veille.rss-feeds (voir {@link TechWatchService}).
 */
public class RssFeedSource implements TechFeedSource {

    private final String name;
    private final String url;
    private final RestClient http;

    public RssFeedSource(String name, String url, int timeoutSeconds) {
        this.name = name;
        this.url = url;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<TechArticleDto> fetch() {
        String xml = http.get()
                .uri(url)
                .header("User-Agent", "PGDI2026-Veille/1.0")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml")
                .retrieve()
                .body(String.class);
        if (xml == null || xml.isBlank()) {
            throw new IllegalStateException("Flux vide");
        }
        return FeedParser.parse(xml, name);
    }
}
