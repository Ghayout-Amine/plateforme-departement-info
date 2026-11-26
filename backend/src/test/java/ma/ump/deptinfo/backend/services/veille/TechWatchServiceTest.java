package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import ma.ump.deptinfo.backend.dtos.TechWatchResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TechWatchServiceTest {

    /** Source de test : renvoie des articles ou lève une erreur, et compte ses appels. */
    private static class FakeSource implements TechFeedSource {
        private final String name;
        private final List<TechArticleDto> articles;
        boolean failing;
        int calls;

        FakeSource(String name, List<TechArticleDto> articles) {
            this.name = name;
            this.articles = articles;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public List<TechArticleDto> fetch() {
            calls++;
            if (failing) throw new IllegalStateException("timeout");
            return articles;
        }
    }

    /** Horloge réglable pour simuler le temps qui passe. */
    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-25T10:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static TechArticleDto article(String link, int day) {
        return new TechArticleDto("Article " + link, "résumé", link, null,
                OffsetDateTime.of(2026, 11, day, 8, 0, 0, 0, ZoneOffset.UTC), "Test");
    }

    @Test
    @DisplayName("Fusion des sources, doublons retirés, du plus récent au plus ancien")
    void mergesSortsAndDeduplicates() {
        FakeSource devto = new FakeSource("Dev.to", List.of(article("https://a/1", 20), article("https://a/2", 24)));
        FakeSource rss = new FakeSource("RSS", List.of(article("https://a/1", 20), article("https://a/3", 22)));
        TechWatchService service = new TechWatchService(List.of(devto, rss), Duration.ofMinutes(30), new MutableClock());

        TechWatchResponseDTO response = service.getArticles(null);

        assertEquals(3, response.getArticles().size());
        assertEquals("https://a/2", response.getArticles().get(0).getLink());
        assertTrue(response.getUnavailableSources().isEmpty());
    }

    @Test
    @DisplayName("Une source en panne est signalée, les autres articles s'affichent")
    void partialFailure() {
        FakeSource devto = new FakeSource("Dev.to", List.of(article("https://a/1", 20)));
        FakeSource rss = new FakeSource("Le Monde Informatique", List.of());
        rss.failing = true;
        TechWatchService service = new TechWatchService(List.of(devto, rss), Duration.ofMinutes(30), new MutableClock());

        TechWatchResponseDTO response = service.getArticles(20);

        assertEquals(1, response.getArticles().size());
        assertEquals(List.of("Le Monde Informatique"), response.getUnavailableSources());
    }

    @Test
    @DisplayName("Cache : pas de nouvel appel pendant 30 min, puis rafraîchissement")
    void cacheIsUsed() {
        MutableClock clock = new MutableClock();
        FakeSource devto = new FakeSource("Dev.to", List.of(article("https://a/1", 20)));
        TechWatchService service = new TechWatchService(List.of(devto), Duration.ofMinutes(30), clock);

        service.getArticles(null);
        clock.now = clock.now.plus(Duration.ofMinutes(10));
        service.getArticles(null);
        assertEquals(1, devto.calls);

        clock.now = clock.now.plus(Duration.ofMinutes(25));
        service.getArticles(null);
        assertEquals(2, devto.calls);
    }

    @Test
    @DisplayName("Toutes les sources en panne : dernier résultat connu, puis pause de 2 min avant de réessayer")
    void allSourcesDownUsesLastResult() {
        MutableClock clock = new MutableClock();
        FakeSource devto = new FakeSource("Dev.to", List.of(article("https://a/1", 20)));
        TechWatchService service = new TechWatchService(List.of(devto), Duration.ofMinutes(30), clock);
        service.getArticles(null);

        clock.now = clock.now.plus(Duration.ofMinutes(31));
        devto.failing = true;
        TechWatchResponseDTO response = service.getArticles(null);

        assertEquals(1, response.getArticles().size(), "dernier résultat renvoyé");
        assertEquals(List.of("Dev.to"), response.getUnavailableSources());

        clock.now = clock.now.plus(Duration.ofMinutes(1));
        service.getArticles(null);
        assertEquals(2, devto.calls, "pas de nouvel appel pendant la pause");
    }

    @Test
    @DisplayName("Le paramètre limit est borné (1 à 50)")
    void limitIsBounded() {
        List<TechArticleDto> many = new ArrayList<>();
        for (int i = 0; i < 60; i++) many.add(article("https://a/" + i, 1 + i % 28));
        FakeSource source = new FakeSource("Dev.to", many);
        TechWatchService service = new TechWatchService(List.of(source), Duration.ofMinutes(30), new MutableClock());

        assertEquals(5, service.getArticles(5).getArticles().size());
        assertEquals(TechWatchService.MAX_LIMIT, service.getArticles(500).getArticles().size());
        assertEquals(TechWatchService.DEFAULT_LIMIT, service.getArticles(0).getArticles().size());
    }

    @Test
    @DisplayName("Configuration : Dev.to et flux RSS « Nom|URL » ; entrées invalides ignorées")
    void buildSourcesFromProperties() {
        List<TechFeedSource> sources = TechWatchService.buildSources(true, "https://dev.to/api/articles", "",
                "Le Monde Informatique|https://www.lemondeinformatique.fr/rss.xml; mauvais format ;Pirate|ftp://x", 6);

        assertEquals(2, sources.size());
        assertEquals("Dev.to", sources.get(0).name());
        assertEquals("Le Monde Informatique", sources.get(1).name());
    }

    @Test
    @DisplayName("Recherche et filtre par source sur les articles en cache")
    void filtersByQueryAndSource() {
        TechArticleDto a1 = new TechArticleDto("Sécurité réseau et IA", "Détection d'intrusion",
                "https://arxiv.org/abs/1", null, OffsetDateTime.of(2026, 11, 25, 10, 0, 0, 0, ZoneOffset.UTC),
                "arXiv Informatique");
        TechArticleDto a2 = new TechArticleDto("Calcul quantique IBM", "Algorithmes quantiques",
                "https://sciencedaily.com/1", null, OffsetDateTime.of(2026, 11, 24, 10, 0, 0, 0, ZoneOffset.UTC),
                "ScienceDaily Informatique");
        FakeSource s1 = new FakeSource("arXiv Informatique", List.of(a1));
        FakeSource s2 = new FakeSource("ScienceDaily Informatique", List.of(a2));
        TechWatchService service = new TechWatchService(List.of(s1, s2), Duration.ofMinutes(30), new MutableClock());

        TechWatchResponseDTO byQuery = service.getArticles(20, "securite", null);
        assertEquals(1, byQuery.getArticles().size());
        assertEquals("https://arxiv.org/abs/1", byQuery.getArticles().get(0).getLink());

        TechWatchResponseDTO bySource = service.getArticles(20, null, "ScienceDaily Informatique");
        assertEquals(1, bySource.getArticles().size());
        assertEquals("https://sciencedaily.com/1", bySource.getArticles().get(0).getLink());
    }
}
