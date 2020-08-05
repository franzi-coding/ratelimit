package com.airtasker.challenge.ratelimiter;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RateLimitFilter extends OncePerRequestFilter {


    private final RateLimitStrategy strategy;

    public RateLimitFilter(RateLimitStrategy strategy) {

        this.strategy = strategy;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        RateLimitResult rateLimitResult = strategy.checkRateLimit(request.getRemoteAddr());

        if (!rateLimitResult.isRequestAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Content-Type", MediaType.TEXT_HTML_VALUE);
            response.setHeader("Retry-After", String.valueOf(rateLimitResult.getNextAllowedRequestTimeInSeconds()));
            response.getWriter().write("Try again in " + rateLimitResult.getNextAllowedRequestTimeInSeconds() + " seconds");
        } else {
            filterChain.doFilter(request, response);
        }

    }
}
