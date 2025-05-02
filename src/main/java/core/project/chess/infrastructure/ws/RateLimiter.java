package core.project.chess.infrastructure.ws;

import core.project.chess.domain.user.entities.User;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@ApplicationScoped
public class RateLimiter {

    @ConfigProperty(name = "ws.rate.limit")
    int rateLimit;

    private final ConcurrentHashMap<UUID, SWRateLimiter> limiters = new ConcurrentHashMap<>();

    public boolean tryAcquire(User user) {
        UUID key = user.id();
        SWRateLimiter limiter = limiters.computeIfAbsent(key, k -> new SWRateLimiter(rateLimit));
        return limiter.tryAcquire();
    }

    static class SWRateLimiter {
        final int threshold;
        final long windowUnit = 1000L;
        final ConcurrentLinkedQueue<Long> log = new ConcurrentLinkedQueue<>();

        /**
         * Constructs a SlidingWindowRateLimiter with the specified threshold.
         *
         * @param threshold the maximum number of requests allowed per window.
         */
        SWRateLimiter(int threshold) {
            this.threshold = threshold;
        }

        /**
         * Tries to acquire permission for a request based on the rate limit.
         *
         * @return true if the request is within the rate limit; false otherwise.
         */
        boolean tryAcquire() {
            long currentTime = System.currentTimeMillis();
            while (!log.isEmpty() && currentTime - log.peek() > windowUnit) log.poll();

            if (log.size() < threshold) {
                log.offer(currentTime);
                return true;
            }
            return false;
        }
    }
}