package core;

import model.RateLimitConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TokenBucketRateLimiter extends RateLimiter {
    private final Map<String, Integer> tokens = new ConcurrentHashMap<>();
    private final Map<String, Long> lastRefillTime = new HashMap<>();

    public TokenBucketRateLimiter(RateLimitConfig config) {
        super(config, RateLimitType.TOKEN_BUCKET);
    }

    @Override
    public boolean allowRequest(String userId) {
        AtomicBoolean allowed = new AtomicBoolean(false);
        long now = System.currentTimeMillis();

        tokens.compute(userId, (id, availableTokens) -> {
            int currentTokens = refillTokens(userId, now);

            if(currentTokens > 0) {
                allowed.set(true);
                return currentTokens - 1;
            } else {
                return currentTokens;
            }
        });
        return allowed.get();
    }

    private int refillTokens(String userId, long now){
        double refillRate = (double) config.getWindowInSeconds() / config.getMaxRequests();

        long lastRefill = lastRefillTime.getOrDefault(userId, now);
        long elapsedSeconds = (now - lastRefill)/1000;
        int refillTokens = (int) (elapsedSeconds * refillRate);

        int currentTokens = tokens.getOrDefault(userId, config.getMaxRequests());
        currentTokens = Math.min(config.getMaxRequests(), currentTokens+refillTokens);
        if(refillTokens > 0) lastRefillTime.put(userId, now);

        return currentTokens;
    }
}
