package interswitch.academy.verve_guard.services;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimiterService {

    private final Cache<String, List<Long>> requestCache;

    //TODO: Make these configurable
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long WINDOW_MS = 60_000;

    public RateLimiterService() {
        this.requestCache = Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.MINUTES)
                .build();
    }

    public boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW_MS;

        List<Long> timestamps = requestCache.get(ip, k -> new ArrayList<>());

        synchronized (timestamps) {
            timestamps.removeIf(t -> t < windowStart);
            timestamps.add(now);
            return timestamps.size() > MAX_REQUESTS_PER_MINUTE;
        }
    }
}