package com.airtasker.challenge.ratelimiter;

public interface RateLimitStrategy {

    RateLimitResult checkRateLimit(String userIP);
}
