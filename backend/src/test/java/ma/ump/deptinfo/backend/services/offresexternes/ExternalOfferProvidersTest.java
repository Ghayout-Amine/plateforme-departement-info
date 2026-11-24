package ma.ump.deptinfo.backend.services.offresexternes;

import com.sun.net.httpserver.HttpServer;
import ma.ump.deptinfo.backend.dtos.JobOfferDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** PGDI2026-86 : format des requêtes envoyées aux API et lecture de leurs réponses. */
class ExternalOfferProvidersTest {

    private HttpServer server;

    @AfterEach
    void stop() {
        if (server != null) server.stop(0);
    }

    /** Petit serveur HTTP local qui joue le rôle de l'API (aucun appel Internet pendant les tests). */
    private String fakeApi(String json, AtomicReference<String> request) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            request.set(exchange.getRequestMethod() + " " + exchange.getRequestURI() + " "
                    + exchange.getRequestHeaders().getFirst("X-RapidAPI-Host") + " " + body);
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test
    @DisplayName("Jooble : POST /api/{clé} avec keywords et location, réponse transformée en offres")
    void joobleRequestAndResponse() throws IOException {
        AtomicReference<String> request = new AtomicReference<>();
        String base = fakeApi("""
                {"totalCount": 2, "jobs": [
                  {"id": 123, "title": "Stage PFE <b>Java</b> Spring", "company": "Acme", "location": "Casablanca",
                   "snippet": "&nbsp;...stage de <b>6 mois</b> en développement&nbsp;...", "link": "https://ma.jooble.org/desc/123",
                   "updated": "2026-11-20T08:00:00.0000000", "salary": "", "source": "acme.ma", "type": "Stage"},
                  {"id": 124, "title": "Lien dangereux", "link": "javascript:alert(1)"}
                ]}""", request);
        JoobleProvider jooble = new JoobleProvider("CLE-TEST", base + "/api/", 5);

        List<JobOfferDto> offers = jooble.search("stage informatique", "Maroc");

        assertTrue(request.get().startsWith("POST /api/CLE-TEST "));
        assertTrue(request.get().contains("\"keywords\":\"stage informatique\""));
        assertTrue(request.get().contains("\"location\":\"Maroc\""));
        assertEquals(1, offers.size(), "l'offre sans lien http(s) est écartée");
        JobOfferDto o = offers.get(0);
        assertEquals("jooble-123", o.getId());
        assertEquals("Stage PFE Java Spring", o.getTitle());
        assertEquals("...stage de 6 mois en développement ...", o.getSnippet());
        assertEquals("https://ma.jooble.org/desc/123", o.getUrl());
        assertEquals("2026-11-20", o.getPostedDate());
        assertEquals("Jooble", o.getSource());
    }

    @Test
    @DisplayName("JSearch : GET /search?query=… avec les en-têtes RapidAPI")
    void jsearchRequestAndResponse() throws IOException {
        AtomicReference<String> request = new AtomicReference<>();
        String base = fakeApi("""
                {"status": "OK", "data": [
                  {"job_id": "abc", "job_title": "Data Engineer Intern", "employer_name": "Beta",
                   "job_city": "Rabat", "job_country": "MA", "job_apply_link": "https://beta.ma/jobs/abc",
                   "job_description": "Python, SQL, Airflow.", "job_posted_at_datetime_utc": "2026-11-21T00:00:00.000Z"}
                ]}""", request);
        JSearchProvider jsearch = new JSearchProvider("CLE", base, 5);

        List<JobOfferDto> offers = jsearch.search("stage data", "Maroc");

        assertTrue(request.get().startsWith("GET /search?query=stage%20data%20in%20Maroc"), request.get());
        assertTrue(request.get().contains("jsearch.p.rapidapi.com"));
        assertEquals("Rabat, MA", offers.get(0).getLocation());
        assertEquals("2026-11-21", offers.get(0).getPostedDate());
        assertEquals("JSearch", offers.get(0).getSource());
    }

    @Test
    @DisplayName("sans clé, les connecteurs se déclarent non configurés ; réponse vide ou inattendue → liste vide")
    void notConfiguredAndEmptyResponses() {
        assertFalse(new JoobleProvider("  ", "https://jooble.org/api/", 5).isConfigured());
        assertFalse(new JSearchProvider("", "https://jsearch.p.rapidapi.com", 5).isConfigured());
        assertTrue(JoobleProvider.parse(null).isEmpty());
        assertTrue(JoobleProvider.parse(Map.of("jobs", "pas une liste")).isEmpty());
        assertTrue(JSearchProvider.parse(Map.of("status", "ERROR")).isEmpty());
    }

    @Test
    @DisplayName("résumé : HTML retiré et texte coupé proprement")
    void plainTextSnippet() {
        assertEquals("a & b", OfferText.plain("<p>a &amp; b</p>", 100));
        assertTrue(OfferText.plain("mot ".repeat(200), 50).endsWith("…"));
        assertEquals("", OfferText.isoDate("hier"));
    }
}
