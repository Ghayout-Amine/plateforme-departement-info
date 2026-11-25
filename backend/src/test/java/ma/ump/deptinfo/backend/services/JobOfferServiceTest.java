package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.JobOfferDto;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.services.offresexternes.ExternalOfferProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** PGDI2026-86 : cache local, repli en cas de panne, exemples sans clé, dédoublonnage. */
class JobOfferServiceTest {

    private ExternalOfferProvider jooble;
    private ExternalOfferProvider jsearch;
    private MutableClock clock;
    private JobOfferService service;

    @BeforeEach
    void setUp() {
        jooble = mock(ExternalOfferProvider.class);
        jsearch = mock(ExternalOfferProvider.class);
        when(jooble.name()).thenReturn("Jooble");
        when(jsearch.name()).thenReturn("JSearch");
        when(jooble.isConfigured()).thenReturn(true);
        when(jsearch.isConfigured()).thenReturn(false);
        clock = new MutableClock(Instant.parse("2026-11-24T10:00:00Z"));
        service = new JobOfferService(List.of(jooble, jsearch), Duration.ofMinutes(30), clock);
    }

    private static JobOfferDto offer(String id, String title, String company, String url, String date) {
        return JobOfferDto.builder().id(id).title(title).company(company).location("Casablanca")
                .url(url).snippet("…").source("Jooble").postedDate(date).build();
    }

    @Test
    @DisplayName("valeurs par défaut : « stage informatique » au Maroc ; seul le fournisseur configuré est appelé")
    void defaultsAndConfiguredProvidersOnly() {
        when(jooble.search("stage informatique", "Maroc")).thenReturn(List.of(offer("1", "Stage Java", "A", "https://x/1", "2026-11-20")));

        List<JobOfferDto> result = service.searchOffers(null, "  ");

        assertEquals(1, result.size());
        verify(jooble).search("stage informatique", "Maroc");
        verify(jsearch, never()).search(anyString(), anyString());
    }

    @Test
    @DisplayName("cache : une même recherche dans les 30 minutes n'appelle pas l'API une deuxième fois")
    void cachesResultsToSaveQuota() {
        when(jooble.search("stage java", "Rabat")).thenReturn(List.of(offer("1", "Stage Java", "A", "https://x/1", "2026-11-20")));

        service.searchOffers("stage java", "Rabat");
        service.searchOffers("  Stage   Java ", "rabat");          // même clé après nettoyage
        verify(jooble, times(1)).search(anyString(), anyString());

        clock.advance(Duration.ofMinutes(31));
        service.searchOffers("stage java", "Rabat");
        verify(jooble, times(2)).search(anyString(), anyString());
    }

    @Test
    @DisplayName("API en panne : le dernier résultat connu est renvoyé")
    void fallsBackToLastKnownResult() {
        when(jooble.search(anyString(), anyString()))
                .thenReturn(List.of(offer("1", "Stage Java", "A", "https://x/1", "2026-11-20")))
                .thenThrow(new IllegalStateException("429 Too Many Requests"));

        service.searchOffers("stage java", "Rabat");
        clock.advance(Duration.ofHours(2));
        List<JobOfferDto> result = service.searchOffers("stage java", "Rabat");

        assertEquals("Stage Java", result.get(0).getTitle());
    }

    @Test
    @DisplayName("aucune clé configurée : exemples marqués « Exemple » avec un lien de recherche Jooble")
    void examplesWithoutApiKey() {
        when(jooble.isConfigured()).thenReturn(false);

        List<JobOfferDto> result = service.searchOffers("stage devops", "Oujda");

        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(o -> "Exemple".equals(o.getSource())));
        assertTrue(result.get(0).getUrl().startsWith("https://jooble.org/SearchResult?ukw=stage+devops"));
        assertEquals("Oujda", result.get(0).getLocation());
        verify(jooble, never()).search(anyString(), anyString());
    }

    @Test
    @DisplayName("deux API, même annonce : un seul exemplaire, les plus récentes d'abord")
    void deduplicatesAndSortsByDate() {
        when(jsearch.isConfigured()).thenReturn(true);
        when(jooble.search(anyString(), anyString())).thenReturn(List.of(
                offer("j1", "Stage Java", "Acme", "https://x/1", "2026-11-10"),
                offer("j2", "Stage Python", "Beta", "https://x/2", "2026-11-22")));
        when(jsearch.search(anyString(), anyString())).thenReturn(List.of(
                offer("s1", "Stage Java", "ACME", "https://autre/1", "2026-11-10"),     // même titre + entreprise
                offer("s2", "Stage React", "Gamma", "https://x/3", "")));

        List<JobOfferDto> result = service.searchOffers("stage", "Maroc");

        assertEquals(List.of("Stage Python", "Stage Java", "Stage React"), result.stream().map(JobOfferDto::getTitle).toList());
    }

    @Test
    @DisplayName("mot-clé de plus de 80 caractères : 400 Bad Request")
    void rejectsTooLongParameters() {
        assertThrows(BadRequestException.class, () -> service.searchOffers("x".repeat(81), "Maroc"));
    }

    /** Horloge réglable pour tester l'expiration du cache sans attendre. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
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
}
