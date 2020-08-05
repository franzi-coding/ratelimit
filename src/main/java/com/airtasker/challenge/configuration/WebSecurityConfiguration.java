package com.airtasker.challenge.configuration;

import com.airtasker.challenge.ratelimiter.RateLimitStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableScheduling
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    RateLimitStrategy rateLimitStrategy;

    private final RateLimitFilterFactory rateLimitFilterFactory;


    public WebSecurityConfiguration(RateLimitFilterFactory rateLimitFilterFactory) {
        this.rateLimitFilterFactory = rateLimitFilterFactory;
    }

    @Override
    public void configure(HttpSecurity http) {

        http.addFilterBefore(rateLimitFilterFactory.creatRateLimitFilter(rateLimitStrategy), BasicAuthenticationFilter.class);

    }


}
