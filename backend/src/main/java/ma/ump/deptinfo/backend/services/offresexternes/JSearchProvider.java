package ma.ump.deptinfo.backend.services.offresexternes;

import ma.ump.deptinfo.backend.dtos.JobOfferDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PGDI2026-86 : connecteur JSearch (RapidAPI, offre gratuite limitée en nombre de requêtes par mois).
 * GET https://jsearch.p.rapidapi.com/search?query=stage informatique in Maroc&page=1&num_pages=1
 * En-têtes : X-RapidAPI-Key, X-RapidAPI-Host. Facultatif : utilisé seulement si la clé est renseignée.
 */
@Component
public class JSearchProvider implements ExternalOfferProvider {

    private static final String HOST = "jsearch.p.rapidapi.com";

    private final String apiKey;
    private final RestClient http;

    public JSearchProvider(@Value("${app.offres-externes.jsearch.api-key:}") String apiKey,
                           @Value("${app.offres-externes.jsearch.url:https://jsearch.p.rapidapi.com}") String baseUrl,
                           @Value("${app.offres-externes.timeout-seconds:8}") int timeoutSeconds) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(timeoutSeconds));
        factory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public String name() {
        return "JSearch";
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty();
    }

    @Override
    public List<JobOfferDto> search(String keyword, String location) {
        String query = location.isBlank() ? keyword : keyword + " in " + location;
        Map<String, Object> response = http.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("query", query)
                        .queryParam("page", 1)
                        .queryParam("num_pages", 1)
                        .queryParam("date_posted", "month")
                        .build())
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", HOST)
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() { });
        return parse(response);
    }

    /** Transforme la réponse JSON de JSearch ({"status": "OK", "data": [...]}) en offres. */
    static List<JobOfferDto> parse(Map<String, Object> response) {
        List<JobOfferDto> offers = new ArrayList<>();
        if (response == null || !(response.get("data") instanceof List<?> data)) return offers;
        for (Object item : data) {
            if (!(item instanceof Map<?, ?> job)) continue;
            String title = OfferText.title(OfferText.str(job, "job_title"));
            String link = OfferText.str(job, "job_apply_link");
            if (title.isEmpty() || !OfferText.isHttpLink(link)) continue;
            String city = OfferText.str(job, "job_city");
            String country = OfferText.str(job, "job_country");
            String location = city.isEmpty() ? country : (country.isEmpty() ? city : city + ", " + country);
            offers.add(JobOfferDto.builder()
                    .id("jsearch-" + OfferText.str(job, "job_id"))
                    .title(title)
                    .company(OfferText.plain(OfferText.str(job, "employer_name"), 150))
                    .location(location)
                    .url(link)
                    .snippet(OfferText.snippet(OfferText.str(job, "job_description"), 400))
                    .source("JSearch")
                    .postedDate(OfferText.isoDate(OfferText.str(job, "job_posted_at_datetime_utc")))
                    .build());
        }
        return offers;
    }
}
