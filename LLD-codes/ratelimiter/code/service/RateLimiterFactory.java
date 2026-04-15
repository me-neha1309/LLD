package service;

import core.*;
import model.RateLimitConfig;

public class RateLimiterFactory {
    public static RateLimiter createRateLimiter(RateLimitType type, RateLimitConfig config){
        return switch(type) {
            case FIXED_WINDOW -> new FixedWindowRateLimiter(config);
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(config);
            case SLIDING_WINDOW_LOG -> new SlidingWindowLogRateLimiter(config);
            default -> throw new UnsupportedOperationException("Type not implemented yet");
        };
    }
}
