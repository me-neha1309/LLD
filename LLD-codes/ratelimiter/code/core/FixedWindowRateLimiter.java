package core;

import model.RateLimitConfig;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class FixedWindowRateLimiter extends RateLimiter {

    private final Map<String, AtomicInteger> requestCount = new ConcurrentHashMap<>();
    private final Map<String, Long> windowStart = new ConcurrentHashMap<>();

    private final long windowSizeMillis;

    public FixedWindowRateLimiter(RateLimitConfig config) {
        super(config, RateLimitType.FIXED_WINDOW);
        this.windowSizeMillis = config.getWindowInSeconds() * 1000L;
    }

    @Override
    public boolean allowRequest(String userId) {
        long now = System.currentTimeMillis();

        long currentWindow = (now / windowSizeMillis) * windowSizeMillis;

        windowStart.compute(userId, (id, prevWindow) -> {
            if (prevWindow == null || prevWindow != currentWindow) {
                requestCount.put(id, new AtomicInteger(0));
                return currentWindow;
            }
            return prevWindow;
        });

        AtomicInteger counter =
                requestCount.computeIfAbsent(userId, k -> new AtomicInteger(0));

        int currentCount = counter.incrementAndGet();
        return currentCount <= config.getMaxRequests();
    }
}
