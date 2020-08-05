package com.airtasker.challenge;

import com.airtasker.challenge.util.TimeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {RatelimiterApplication.class})
@TestPropertySource(properties = {"api.limit.in.minutes=2", "api.limit.requests=2", "schedule.cleanup.rate.limit.data=0 0 18 27 1 ?", "rate.limit.cleanup.minutes=1440"})
@WebAppConfiguration
@ExtendWith(SpringExtension.class)
public class RatelimiterApplicationTests {

    @Autowired
    private WebApplicationContext wac;

    @MockBean
    TimeProvider timeProvider;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(LocalDateTime.now());
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).apply(springSecurity()).build();
    }

    @Test
    public void shouldAllow_WhenWithinLimitOfRequests_And_WithinTimeWindow() throws Exception {
        mockMvc.perform(get("/").with(ip("127.0.0.1")))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello, Successfully retrieved request"));

    }

    @Test
    public void shouldNotAllow_WhenMoreThanAllowedRequests_WithinTimeWindow() throws Exception {
        String userIp = "127.0.0.2";
        mockMvc.perform(get("/").with(ip(userIp))).andExpect(status().isOk());
        mockMvc.perform(get("/").with(ip(userIp))).andExpect(status().isOk());

        mockMvc.perform(get("/").with(ip(userIp)))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string("Try again in 120 seconds"));
    }

    @Test
    public void shouldAllow_WhenMoreThanAllowedRequests_AndNotInTimeWindow() throws Exception {

        String userIp = "127.0.0.3";

        //First Request
        LocalDateTime timeOfFirstRequest = LocalDateTime.now();

        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest);
        mockMvc.perform(get("/").with(ip(userIp))).andExpect(status().isOk());

        //Second Request
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(1));
        mockMvc.perform(get("/").with(ip(userIp))).andExpect(status().isOk());

        //ThirdRequest
        Mockito.when(timeProvider.getCurrentTime()).thenReturn(timeOfFirstRequest.plusMinutes(5));
        mockMvc.perform(get("/").with(ip(userIp))).andExpect(status().isOk());
    }


    @Test
    public void shouldAllow_WhenWithinLimitOfRequests_AndWithinTimeWindow_WithSecondUser() throws Exception {
        String firstUserIp = "127.0.0.4";
        String secondUserIp = "127.0.0.5";

        mockMvc.perform(get("/").with(ip(firstUserIp))).andExpect(status().isOk());
        mockMvc.perform(get("/").with(ip(secondUserIp))).andExpect(status().isOk());
        mockMvc.perform(get("/").with(ip(firstUserIp))).andExpect(status().isOk());
    }

    @Test
    public void shouldAllowSecondUser_WhenFirstUserReachedLimit() throws Exception {
        String firstUserIp = "127.0.0.6";
        String secondUserIp = "127.0.0.7";

        mockMvc.perform(get("/").with(ip(firstUserIp))).andExpect(status().isOk());
        mockMvc.perform(get("/").with(ip(firstUserIp))).andExpect(status().isOk());
        mockMvc.perform(get("/").with(ip(firstUserIp))).andExpect(status().is4xxClientError());

        mockMvc.perform(get("/").with(ip(secondUserIp))).andExpect(status().isOk());

    }


    RequestPostProcessor ip(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

}
