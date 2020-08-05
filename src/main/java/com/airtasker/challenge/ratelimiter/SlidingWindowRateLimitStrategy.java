package com.airtasker.challenge.ratelimiter;

import com.airtasker.challenge.util.TimeProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SlidingWindowRateLimitStrategy implements RateLimitStrategy {

    @Value("${rate.limit.cleanup.minutes}")
    int timeToCleanupInMinutes;

    private ConcurrentHashMap<String, Queue<LocalDateTime>> ipToRequestsMap;
    private final TimeProvider timeProvider;
    private final int apiLimitInMinutes;
    private final int apiLimitRequests;


    public SlidingWindowRateLimitStrategy(TimeProvider timeProvider, @Value("${api.limit.in.minutes}") int apiLimitInMinutes,
                                          @Value("${api.limit.requests}") int apiLimitRequests) {
        this.timeProvider = timeProvider;
        this.apiLimitInMinutes = apiLimitInMinutes;
        this.apiLimitRequests = apiLimitRequests;
        this.ipToRequestsMap = new ConcurrentHashMap<>();
    }


    @Override
    public RateLimitResult checkRateLimit(String userIP) {

        LocalDateTime currentTime = timeProvider.getCurrentTime();

        if (!ipToRequestsMap.containsKey(userIP)) {
            addRequestToMap(userIP, currentTime, new ArrayBlockingQueue<>(apiLimitRequests));
            return new RateLimitResult(true, apiLimitRequests - 1);
        }

        Queue requestQueue = ipToRequestsMap.get(userIP);

        cleanExpiredRequestsInQueue(currentTime, requestQueue);

        if (requestQueue.size() >= apiLimitRequests) {
            return new RateLimitResult(false, calculateNextAllowedRequestTimeInSeconds(currentTime, requestQueue), 0);
        } else {
            if (addRequestToMap(userIP, currentTime, requestQueue)) {
                return new RateLimitResult(true, apiLimitRequests - requestQueue.size());
            }
            return new RateLimitResult(false, apiLimitInMinutes * 60, 0);
        }

    }

    private long calculateNextAllowedRequestTimeInSeconds(LocalDateTime currentTime, Queue queueOfRequests) {
        LocalDateTime timeOfFirstRequest = (LocalDateTime) queueOfRequests.peek();
        Duration diff = Duration.between(currentTime, timeOfFirstRequest.plusMinutes(apiLimitInMinutes));

        return diff.getSeconds();
    }


    private void cleanExpiredRequestsInQueue(LocalDateTime currentTime, Queue<LocalDateTime> queue) {

        LocalDateTime expirationTime = currentTime.minusMinutes(apiLimitInMinutes);

        while (firstEntryExpired(queue, expirationTime)) {
            queue.poll();
        }

    }

    private boolean firstEntryExpired(Queue<LocalDateTime> queue, LocalDateTime expirationTime) {
        return queue.peek() != null && expirationTime.isAfter(queue.peek());
    }

    private boolean addRequestToMap(String userIP, LocalDateTime currentTime, Queue<LocalDateTime> requestTimeQueue) {

        if (requestTimeQueue.offer(currentTime)) {
            ipToRequestsMap.put(userIP, requestTimeQueue);
            return true;
        }

        return false;
    }


    @Scheduled(cron = "${schedule.cleanup.rate.limit.data}")
    public void scheduleCleanupTask() {

        System.out.println("rate limit cleaning job started, cleaning entries older than " + timeToCleanupInMinutes + " minutes");

        if (ipToRequestsMap.size() == 0) {
            return;
        }

        deleteMapEntriesOlderThanInMinutes(ipToRequestsMap, timeToCleanupInMinutes);
    }

    protected ConcurrentHashMap<String, Queue<LocalDateTime>> deleteMapEntriesOlderThanInMinutes(ConcurrentHashMap<String, Queue<LocalDateTime>> ipMap, int minutes) {
        ipMap.entrySet().removeIf(entry -> isOlderThan(entry, minutes));
        return ipMap;
    }

    private boolean isOlderThan(Map.Entry<String, Queue<LocalDateTime>> entry, int minutes) {
        LocalDateTime firstRequestTime = entry.getValue().peek();
        return firstRequestTime == null || firstRequestTime.isBefore(timeProvider.getCurrentTime().minusMinutes(minutes));
    }
}
