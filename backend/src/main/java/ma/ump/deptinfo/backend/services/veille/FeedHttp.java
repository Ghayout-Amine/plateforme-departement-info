package ma.ump.deptinfo.backend.services.veille;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Utilitaire HTTP partagé entre les connecteurs de veille technologique (timeouts + 1 nouvelle tentative).
 */
final class FeedHttp {

    private static final int MAX_CONNECT_SECONDS = 8;

    private FeedHttp() {
    }

    static RestClient createClient(int timeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(Math.max(1, Math.min(timeoutSeconds, MAX_CONNECT_SECONDS))));
        factory.setReadTimeout(Duration.ofSeconds(Math.max(1, timeoutSeconds)));
        return RestClient.builder().requestFactory(factory).build();
    }

    /** Réessaie une fois en cas d'erreur réseau transitoire (ex. timeout TCP ponctuel). */
    static <T> T getWithRetry(Supplier<T> action) {
        try {
            return action.get();
        } catch (RuntimeException firstFailure) {
            return action.get();
        }
    }
}
