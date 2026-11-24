package ma.ump.deptinfo.backend.services;

import ma.ump.deptinfo.backend.dtos.JobOfferDto;
import ma.ump.deptinfo.backend.exceptions.BadRequestException;
import ma.ump.deptinfo.backend.services.offresexternes.ExternalOfferProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PGDI2026-86 : recherche d'offres de stage dans les API externes (Jooble, JSearch).
 * <ul>
 *   <li>cache local en mémoire (30 min par défaut) par couple mot-clé / lieu, pour préserver les quotas gratuits ;</li>
 *   <li>si une API échoue, le dernier résultat en cache est renvoyé, même périmé ;</li>
 *   <li>sans aucune clé configurée, une courte liste d'exemples (source « Exemple ») avec un lien vers la recherche Jooble.</li>
 * </ul>
 */
@Service
public class JobOfferService {

    private static final Logger log = LoggerFactory.getLogger(JobOfferService.class);
    static final String DEFAULT_KEYWORD = "stage informatique";
    static final String DEFAULT_LOCATION = "Maroc";
    private static final int MAX_CACHE_ENTRIES = 200;
    private static final int MAX_PARAM_LENGTH = 80;

    private final List<ExternalOfferProvider> providers;
    private final Duration cacheTtl;
    private final Clock clock;
    private final Map<String, CachedResult> cache = new ConcurrentHashMap<>();

    private record CachedResult(List<JobOfferDto> offers, Instant storedAt) {
    }

    @Autowired
    public JobOfferService(List<ExternalOfferProvider> providers,
                           @Value("${app.offres-externes.cache-minutes:30}") long cacheMinutes) {
        this(providers, Duration.ofMinutes(cacheMinutes), Clock.systemDefaultZone());
    }

    JobOfferService(List<ExternalOfferProvider> providers, Duration cacheTtl, Clock clock) {
        this.providers = providers;
        this.cacheTtl = cacheTtl;
        this.clock = clock;
    }

    public List<JobOfferDto> searchOffers(String keyword, String location) {
        String kw = clean(keyword, DEFAULT_KEYWORD);
        String loc = clean(location, DEFAULT_LOCATION);
        String key = (kw + "|" + loc).toLowerCase(Locale.ROOT);

        CachedResult cached = cache.get(key);
        if (cached != null && cached.storedAt().plus(cacheTtl).isAfter(clock.instant())) {
            return cached.offers();
        }

        List<ExternalOfferProvider> active = providers.stream().filter(ExternalOfferProvider::isConfigured).toList();
        if (active.isEmpty()) {
            log.info("Aucune clé d'API d'offres (JOOBLE_API_KEY / JSEARCH_API_KEY) : exemples renvoyés");
            return examples(kw, loc);
        }

        List<JobOfferDto> merged = new ArrayList<>();
        int failures = 0;
        for (ExternalOfferProvider provider : active) {
            try {
                merged.addAll(provider.search(kw, loc));
            } catch (RuntimeException e) {
                failures++;
                log.warn("API {} indisponible pour « {} » à « {} » : {}", provider.name(), kw, loc, e.getMessage());
            }
        }

        if (failures == active.size()) {
            // toutes les API ont échoué : dernier résultat connu, sinon les exemples
            return cached != null ? cached.offers() : examples(kw, loc);
        }

        List<JobOfferDto> result = deduplicate(merged);
        if (cache.size() >= MAX_CACHE_ENTRIES) cache.clear();
        cache.put(key, new CachedResult(result, clock.instant()));
        return result;
    }

    /** Valeur nettoyée : espaces compactés, longueur limitée, valeur par défaut si vide. */
    static String clean(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String v = value.trim().replaceAll("\\s+", " ");
        if (v.length() > MAX_PARAM_LENGTH) {
            throw new BadRequestException("Le mot-clé et le lieu sont limités à " + MAX_PARAM_LENGTH + " caractères.");
        }
        return v;
    }

    /** Une même annonce peut venir de deux API : on garde la première (même lien, ou même titre + entreprise). */
    static List<JobOfferDto> deduplicate(List<JobOfferDto> offers) {
        Map<String, JobOfferDto> unique = new LinkedHashMap<>();
        for (JobOfferDto o : offers) {
            String byUrl = "url:" + o.getUrl();
            String byTitle = "t:" + (o.getTitle() + "|" + o.getCompany()).toLowerCase(Locale.ROOT);
            if (unique.containsKey(byUrl) || unique.containsKey(byTitle)) continue;
            unique.put(byUrl, o);
            unique.put(byTitle, o);
        }
        return unique.values().stream().distinct()
                .sorted(Comparator.comparing((JobOfferDto o) -> o.getPostedDate() == null ? "" : o.getPostedDate()).reversed())
                .toList();
    }

    /** Sans clé d'API : exemples fictifs, clairement marqués, avec un vrai lien vers la recherche Jooble. */
    static List<JobOfferDto> examples(String keyword, String location) {
        String search = "https://jooble.org/SearchResult?ukw=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                + "&rgns=" + URLEncoder.encode(location, StandardCharsets.UTF_8);
        return List.of(
                example("1", "Stage PFE — Développeur Full Stack Spring Boot / Angular", "Atlas Numérique (exemple)", location,
                        "Conception de microservices Spring Boot et d'interfaces Angular, intégration continue avec Docker.", search),
                example("2", "Stage Data / IA — tableau de bord et prédictions", "Oriental Data Lab (exemple)", location,
                        "Préparation de données en Python, modèles de machine learning et visualisation.", search),
                example("3", "Stage DevOps — CI/CD et Kubernetes", "CloudOps Maroc (exemple)", location,
                        "Pipelines GitHub Actions, conteneurs Docker, supervision avec Prometheus et Grafana.", search));
    }

    private static JobOfferDto example(String id, String title, String company, String location, String snippet, String url) {
        return JobOfferDto.builder()
                .id("exemple-" + id).title(title).company(company).location(location)
                .url(url).snippet(snippet).source("Exemple").postedDate("")
                .build();
    }
}
