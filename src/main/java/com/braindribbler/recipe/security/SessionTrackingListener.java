package com.braindribbler.recipe.security;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class SessionTrackingListener implements HttpSessionListener {

    private static final Logger log = LoggerFactory.getLogger(SessionTrackingListener.class);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        ServletRequestAttributes attrs = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attrs != null) {
            var request = attrs.getRequest();
            log.info("New session [{}] created for URI: {} | IP: {}",
                    se.getSession().getId(),
                    request.getRequestURI(),
                    request.getRemoteAddr());

            // Optional: Set Locale immediately even for anonymous sessions
            se.getSession().setAttribute("Locale", request.getLocale());
        }
    }
}
