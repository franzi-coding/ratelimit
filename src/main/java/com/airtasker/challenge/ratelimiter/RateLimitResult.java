package com.airtasker.challenge.ratelimiter;


public class RateLimitResult {

    private final boolean requestAllowed;
    private final int remainingAllowedAmountOfRequests;
    private final long nextAllowedRequestTimeInSeconds;

    RateLimitResult(boolean requestAllowed, long nextAllowedRequestTimeInSeconds, int remainingAllowedAmountOfRequests) {

        this.requestAllowed = requestAllowed;
        this.nextAllowedRequestTimeInSeconds = nextAllowedRequestTimeInSeconds;
        this.remainingAllowedAmountOfRequests = remainingAllowedAmountOfRequests;
    }

    RateLimitResult(boolean requestAllowed, int remainingAllowedAmountOfRequests) {

        this.requestAllowed = requestAllowed;
        this.nextAllowedRequestTimeInSeconds = 0;
        this.remainingAllowedAmountOfRequests = remainingAllowedAmountOfRequests;
    }

    public boolean isRequestAllowed() {
        return requestAllowed;
    }

    public long getNextAllowedRequestTimeInSeconds() {
        return nextAllowedRequestTimeInSeconds;
    }

    public int getRemainingAllowedAmountOfRequests() {
        return remainingAllowedAmountOfRequests;
    }

}
