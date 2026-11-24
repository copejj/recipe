package com.braindribbler.recipe.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class SseOpenInViewExclusionConfig {

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> disableOpenInViewForSseFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain) throws ServletException, IOException {
                if (request.getRequestURI().endsWith("/stream")) {
                    request.setAttribute("org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor.FILTERED", Boolean.TRUE);
                }
                filterChain.doFilter(request, response);
            }
        });
        registration.addUrlPatterns("/shopping-lists/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
