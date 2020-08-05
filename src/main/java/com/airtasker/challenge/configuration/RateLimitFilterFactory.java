package com.airtasker.challenge.configuration;

import com.airtasker.challenge.ratelimiter.RateLimitFilter;
import com.airtasker.challenge.ratelimiter.RateLimitStrategy;
import com.airtasker.challenge.util.TimeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RateLimitFilterFactory {


    @Autowired
    TimeProvider timeProvider;

    public RateLimitFilterFactory() {
    }

    public RateLimitFilter creatRateLimitFilter(RateLimitStrategy rateLimitStrategy) {
        return new RateLimitFilter(rateLimitStrategy);
    }
}
