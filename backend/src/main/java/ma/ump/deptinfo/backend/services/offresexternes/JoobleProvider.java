package ma.ump.deptinfo.backend.services.offresexternes;

import ma.ump.deptinfo.backend.dtos.JobOfferDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PGDI2026-86 : connecteur Jooble (gratuit, clé obtenue par e-mail sur jooble.org/api/about).
 * POST https://ma.jooble.org/api/{clé}  corps : {"keywords": "...", "location": "...", "page": 1, "ResultOnPage": 50}
 * Attention : une clé est liée au site Jooble d'un pays ; l'URL doit être celle de ce site
 * (clé demandée sur ma.jooble.org → https://ma.jooble.org/api/, réglable avec JOOBLE_API_URL).
 */
@Component
public class JoobleProvider implements ExternalOfferProvider {

    // 50 résultats par appel (1 seule requête sur le quota) : il en reste assez après le filtre « informatique »
    private static final int RESULTS_PER_PAGE = 50;

    private final String apiKey;
    private final String baseUrl;
    private final RestClient http;

    public JoobleProvider(@Value("${app.offres-externes.jooble.api-key:}") String apiKey,
                          @Value("${app.offres-externes.jooble.url:https://ma.jooble.org/api/}") String baseUrl,
                          @Value("${app.offres-externes.timeout-seconds:8}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.http = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public String name() {
        return "Jooble";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public List<JobOfferDto> search(String keyword, String location) {
        Map<String, Object> body = Map.of(
                "keywords", keyword,
                "location", location,
                "page", 1,
                "ResultOnPage", RESULTS_PER_PAGE);
        Map<String, Object> response = http.post()
                .uri(baseUrl + "{key}", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        return parse(response);
    }

    /** Transforme la réponse JSON de Jooble ({"totalCount": n, "jobs": [...]}) en offres. */
    static List<JobOfferDto> parse(Map<String, Object> response) {
        List<JobOfferDto> offers = new ArrayList<>();
        if (response == null || !(response.get("jobs") instanceof List<?> jobs)) return offers;
        for (Object item : jobs) {
            if (!(item instanceof Map<?, ?> job)) continue;
            String title = OfferText.title(OfferText.str(job, "title"));
            String link = OfferText.str(job, "link");
            if (title.isEmpty() || !OfferText.isHttpLink(link)) continue;
            offers.add(JobOfferDto.builder()
                    .id("jooble-" + OfferText.str(job, "id"))
                    .title(title)
                    .company(OfferText.plain(OfferText.str(job, "company"), 150))
                    .location(OfferText.plain(OfferText.str(job, "location"), 150))
                    .url(link)
                    .snippet(OfferText.snippet(OfferText.str(job, "snippet"), 400))
                    .source("Jooble")
                    .postedDate(OfferText.isoDate(OfferText.str(job, "updated")))
                    .build());
        }
        return offers;
    }
}
