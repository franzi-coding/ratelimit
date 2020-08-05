package com.airtasker.challenge.ratelimiter;

import com.airtasker.challenge.util.SystemTimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
class SlidingWindowRateLimitStrategyTest {

    private SystemTimeProvider timeProvider;
    private SlidingWindowRateLimitStrategy strategy;
    private String firstUserIP;


    @BeforeEach
    public void setUp() {
        timeProvider = Mockito.mock(SystemTimeProvider.class);
        firstUserIP = "127.0.0.1";
    }


    @Test
    public void shouldAllow_WhenWithinLimitOfRequests_And_WithinTimeWindow() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();

        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(firstUserIP);

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(1));
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);

        assertTrue(result.isRequestAllowed());
    }


    @Test
    public void shouldNotAllow_WhenMoreThanAllowedRequests_WithinTimeWindow() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();

        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(firstUserIP);

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(1));
        strategy.checkRateLimit(firstUserIP);

        //Third Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(2));
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);


        assertFalse(result.isRequestAllowed());
    }

    @Test
    public void shouldCalculateRemainingTimeInSeconds() {

        int limitInMinutes = 1;
        int limitOfRequests = 1;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();

        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(firstUserIP);

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusSeconds(1));
        RateLimitResult resultSecondRequest = strategy.checkRateLimit(firstUserIP);

        assertEquals(59, resultSecondRequest.getNextAllowedRequestTimeInSeconds());

        //Third Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusSeconds(2));
        RateLimitResult resultThirdRequest = strategy.checkRateLimit(firstUserIP);

        assertEquals(58, resultThirdRequest.getNextAllowedRequestTimeInSeconds());
    }

    @Test
    public void shouldAllow_MoreRequests_when_NotInTimeWindow() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(firstUserIP);

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusSeconds(10));
        strategy.checkRateLimit(firstUserIP);

        //Third Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(limitInMinutes).plusSeconds(1));
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);

        assertTrue(result.isRequestAllowed());
    }


    @Test
    public void shouldAllow_WhenWithinAllowedLimitOfRequests_And_NotInTimeWindow() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(firstUserIP);

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(limitInMinutes).plusSeconds(1));
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);

        assertTrue(result.isRequestAllowed());
    }

    @Test
    public void shouldAllow_WhenMoreThanAllowedLimitOfRequests_And_TimeWindowIsFromPreviousDay() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(LocalDateTime.now());
        strategy.checkRateLimit(firstUserIP);

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(LocalDateTime.now().plusSeconds(10));
        strategy.checkRateLimit(firstUserIP);

        //Third Request, next day
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(LocalDateTime.now().plusDays(1).plusSeconds(1));
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);

        assertTrue(result.isRequestAllowed());
    }

    @Test
    public void shouldAllow_WhenWithinLimitOfRequests_And_WithinTimeWindow_WithSecondUser() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //first user
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(firstUserIP);

        //second user
        String secondUserIP = "197.168.154";
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        strategy.checkRateLimit(secondUserIP);

        //first user, second request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(1));
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);

        assertTrue(result.isRequestAllowed());
    }

    @Test
    public void shouldReturnAmountOfAllowedRemainingRequests() {

        int limitInMinutes = 3;
        int limitOfRequests = 2;

        strategy = new SlidingWindowRateLimitStrategy(timeProvider, limitInMinutes, limitOfRequests);

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        RateLimitResult firstRateLimitResult = strategy.checkRateLimit(firstUserIP);

        assertEquals(1, firstRateLimitResult.getRemainingAllowedAmountOfRequests());

        //Second Request
        LocalDateTime timeOfSecondRequest = timeOfFirstRequest.plusSeconds(10);

        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfSecondRequest);
        RateLimitResult secondRateLimitResult = strategy.checkRateLimit(firstUserIP);

        assertEquals(0, secondRateLimitResult.getRemainingAllowedAmountOfRequests());

        //Third Request
        LocalDateTime timeOfThirdRequest = timeOfSecondRequest.plusMinutes(limitInMinutes).plusSeconds(1);
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfThirdRequest);
        RateLimitResult result = strategy.checkRateLimit(firstUserIP);

        assertEquals(1, result.getRemainingAllowedAmountOfRequests());
    }

    @Test
    public void shouldDeleteOldMapEntries() {
        LocalDateTime now = LocalDateTime.now();
        strategy = new SlidingWindowRateLimitStrategy(timeProvider, 3, 3);

        Mockito.when(timeProvider.getCurrentTime()).thenReturn(now);

        ConcurrentHashMap<String, Queue<LocalDateTime>> map = new ConcurrentHashMap<>();

        addRequestToMap(firstUserIP, now.minusDays(2), new ArrayBlockingQueue<>(100), map);
        addRequestToMap(firstUserIP, now.minusDays(1), map.get(firstUserIP), map);

        String secondUser = "127.0.0.2";
        addRequestToMap(secondUser, now.minusMinutes(2), new ArrayBlockingQueue<>(100), map);


        assertEquals(1, strategy.deleteMapEntriesOlderThanInMinutes(map, 60 * 24).size());

    }

    private void addRequestToMap(String userIP, LocalDateTime currentTime, Queue<LocalDateTime> requestTimeQueue, ConcurrentHashMap<String, Queue<LocalDateTime>> map) {
        requestTimeQueue.add(currentTime);
        map.put(userIP, requestTimeQueue);
    }

}