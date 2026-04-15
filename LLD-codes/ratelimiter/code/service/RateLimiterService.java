package service;

import core.RateLimitType;
import core.RateLimiter;
import model.RateLimitConfig;
import model.User;
import model.UserTier;

import java.util.HashMap;
import java.util.Map;

public class RateLimiterService {
    private final Map<UserTier, RateLimiter> rateLimiters = new HashMap<>();

    public RateLimiterService(){
        //configure per-tier limits + algorithms
        rateLimiters.put(
                UserTier.FREE,
                RateLimiterFactory.createRateLimiter(
                        RateLimitType.TOKEN_BUCKET,
                        new RateLimitConfig(10, 60)
                )
        );

        rateLimiters.put(
                UserTier.PREMIUM,
                RateLimiterFactory.createRateLimiter(
                        RateLimitType.SLIDING_WINDOW_LOG,
                        new RateLimitConfig(100, 60)
                )
        );
    }

    public boolean allowRequest(User user){
        RateLimiter limiter = rateLimiters.get(user.getTier());
        if(limiter==null){
            throw new IllegalArgumentException("No limiter configured for tier : " + user.getTier());
        }

        return limiter.allowRequest(user.getUserId());
    }
}
