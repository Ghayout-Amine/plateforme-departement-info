package ma.ump.deptinfo.backend.services.veille;

import ma.ump.deptinfo.backend.dtos.TechArticleDto;
import ma.ump.deptinfo.backend.dtos.TechWatchResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PGDI2026-88 : veille technologique agrégée depuis l'API Dev.to et des flux RSS/Atom.
 * <ul>
 *   <li>cache en mémoire (30 min par défaut) pour ne pas interroger les sources à chaque visite ;</li>
 *   <li>chaque source a un délai maximal (timeout) : une source lente ou en panne est ignorée
 *       et signalée dans unavailableSources, les autres s'affichent quand même ;</li>
 *   <li>si toutes les sources échouent, le dernier résultat connu est renvoyé (même périmé).</li>
 * </ul>
 * Sources réglables dans application.properties (app.veille.*).
 */
@Service
public class TechWatchService {

    private static final Logger log = LoggerFactory.getLogger(TechWatchService.class);
    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final List<TechFeedSource> sources;
    private final Duration cacheTtl;
    private final Clock clock;

    private List<TechArticleDto> cachedArticles;
    private List<String> cachedUnavailable = List.of();
    private Instant cachedAt;
    private Instant lastFailureAt;     // toutes les sources en panne : on attend avant de réessayer
    static final Duration RETRY_AFTER_FAILURE = Duration.ofMinutes(2);

    @Autowired
    public TechWatchService(
            @Value("${app.veille.devto.enabled:true}") boolean devToEnabled,
            @Value("${app.veille.devto.url:https://dev.to/api/articles}") String devToUrl,
            @Value("${app.veille.devto.tag:}") String devToTag,
            @Value("${app.veille.rss-feeds:}") String rssFeeds,
            @Value("${app.veille.timeout-seconds:6}") int timeoutSeconds,
            @Value("${app.veille.cache-minutes:30}") long cacheMinutes) {
        this(buildSources(devToEnabled, devToUrl, devToTag, rssFeeds, timeoutSeconds),
                Duration.ofMinutes(cacheMinutes), Clock.systemDefaultZone());
    }

    TechWatchService(List<TechFeedSource> sources, Duration cacheTtl, Clock clock) {
        this.sources = sources;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    /**
     * rssFeeds : flux séparés par « ; », chacun au format « Nom|URL ».
     * Ex. « Le Monde Informatique|https://www.lemondeinformatique.fr/flux-rss/thematique/toutes-les-actualites/rss.xml »
     */
    static List<TechFeedSource> buildSources(boolean devToEnabled, String devToUrl, String devToTag,
                                             String rssFeeds, int timeoutSeconds) {
        List<TechFeedSource> list = new ArrayList<>();
        if (devToEnabled) {
            list.add(new DevToSource(devToUrl, devToTag, timeoutSeconds));
        }
        if (rssFeeds != null) {
            for (String entry : rssFeeds.split(";")) {
                String[] parts = entry.split("\\|", 2);
                if (parts.length == 2 && FeedParser.isHttpLink(parts[1].trim()) && !parts[0].isBlank()) {
                    list.add(new RssFeedSource(parts[0].trim(), parts[1].trim(), timeoutSeconds));
                }
            }
        }
        return list;
    }

    public synchronized TechWatchResponseDTO getArticles(Integer limit) {
        return getArticles(limit, null, null);
    }

    public synchronized TechWatchResponseDTO getArticles(Integer limit, String query, String sourceFilter) {
        int max = (limit == null || limit < 1) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);

        if (cachedAt != null && cachedAt.plus(cacheTtl).isAfter(clock.instant())) {
            return response(cachedArticles, cachedUnavailable, cachedAt, max, query, sourceFilter);
        }

        // Sources toutes en panne il y a peu : pas de nouvelle tentative (évite d'attendre les timeouts à chaque visite)
        if (lastFailureAt != null && lastFailureAt.plus(RETRY_AFTER_FAILURE).isAfter(clock.instant())) {
            List<TechArticleDto> fallback = cachedArticles != null ? cachedArticles : List.of();
            return response(fallback, sourceNames(), cachedAt, max, query, sourceFilter);
        }

        List<TechArticleDto> merged = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        fetchAllSources(merged, unavailable);

        if (!sources.isEmpty() && unavailable.size() == sources.size()) {
            // toutes les sources ont échoué : dernier résultat connu, sinon liste vide
            lastFailureAt = clock.instant();
            List<TechArticleDto> fallback = cachedArticles != null ? cachedArticles : List.of();
            return response(fallback, unavailable, cachedAt, max, query, sourceFilter);
        }

        lastFailureAt = null;
        List<TechArticleDto> result = deduplicateAndSort(merged);
        cachedArticles = result;
        cachedUnavailable = List.copyOf(unavailable);
        cachedAt = clock.instant();
        return response(result, cachedUnavailable, cachedAt, max, query, sourceFilter);
    }

    private void fetchAllSources(List<TechArticleDto> merged, List<String> unavailable) {
        if (sources.isEmpty()) return;
        if (sources.size() == 1) {
            TechFeedSource single = sources.get(0);
            try {
                merged.addAll(single.fetch());
            } catch (RuntimeException e) {
                unavailable.add(single.name());
                log.warn("Veille : source {} indisponible : {}", single.name(), e.getMessage());
            }
            return;
        }

        ExecutorService pool = Executors.newFixedThreadPool(sources.size());
        try {
            List<CompletableFuture<List<TechArticleDto>>> tasks = sources.stream()
                    .map(source -> CompletableFuture.supplyAsync(source::fetch, pool))
                    .toList();
            for (int i = 0; i < sources.size(); i++) {
                TechFeedSource source = sources.get(i);
                try {
                    merged.addAll(tasks.get(i).join());
                } catch (CompletionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    unavailable.add(source.name());
                    log.warn("Veille : source {} indisponible : {}", source.name(), cause.getMessage());
                } catch (RuntimeException e) {
                    unavailable.add(source.name());
                    log.warn("Veille : source {} indisponible : {}", source.name(), e.getMessage());
                }
            }
        } finally {
            pool.shutdown();
        }
    }

    /** Un même article peut venir de deux sources : on garde le premier lien, puis tri du plus récent au plus ancien. */
    static List<TechArticleDto> deduplicateAndSort(List<TechArticleDto> articles) {
        Map<String, TechArticleDto> unique = new LinkedHashMap<>();
        for (TechArticleDto a : articles) {
            unique.putIfAbsent(a.getLink(), a);
        }
        return unique.values().stream()
                .sorted(Comparator.comparing(TechArticleDto::getPublishedAt,
                        Comparator.nullsLast(Comparator.<OffsetDateTime>reverseOrder())))
                .toList();
    }

    private List<String> sourceNames() {
        return sources.stream().map(TechFeedSource::name).toList();
    }

    private TechWatchResponseDTO response(List<TechArticleDto> articles, List<String> unavailable,
                                          Instant updatedAt, int max, String query, String sourceFilter) {
        List<TechArticleDto> filtered = filter(articles, query, sourceFilter);
        List<TechArticleDto> limited = filtered.size() > max ? filtered.subList(0, max) : filtered;
        OffsetDateTime date = updatedAt == null ? null : OffsetDateTime.ofInstant(updatedAt, clock.getZone());
        return new TechWatchResponseDTO(List.copyOf(limited), List.copyOf(unavailable), date);
    }

    static List<TechArticleDto> filter(List<TechArticleDto> articles, String query, String sourceFilter) {
        String source = sourceFilter == null ? "" : sourceFilter.trim();
        boolean hasSource = !source.isEmpty() && !"ALL".equalsIgnoreCase(source);
        String normalizedQuery = normalize(query);
        String[] terms = normalizedQuery.isEmpty() ? new String[0] : normalizedQuery.split("\\s+");

        if (!hasSource && terms.length == 0) {
            return articles;
        }
        return articles.stream()
                .filter(a -> !hasSource || (a.getSource() != null && a.getSource().equalsIgnoreCase(source)))
                .filter(a -> matchesTerms(a, terms))
                .toList();
    }

    private static boolean matchesTerms(TechArticleDto article, String[] terms) {
        if (terms.length == 0) return true;
        String text = normalize((article.getTitle() == null ? "" : article.getTitle()) + " "
                + (article.getSummary() == null ? "" : article.getSummary()) + " "
                + (article.getSource() == null ? "" : article.getSource()));
        for (String term : terms) {
            if (!text.contains(term)) return false;
        }
        return true;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "";
        String decomposed = Normalizer.normalize(value.trim(), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }
}
