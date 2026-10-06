package com.braindribbler.recipe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
// CRITICAL: Force a global scan over all sub-packages so Spring finds every
// bean dynamically
@ComponentScan(basePackages = "com.braindribbler.recipe")
@PropertySource(value = "classpath:secrets.properties", ignoreResourceNotFound = true)
public class RecipeApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RecipeApplication.class);
    }

    public static void main(String[] args) {
        // Streamlined: Spring now discovers all security, services, and controllers on
        // its own!
        SpringApplication.run(RecipeApplication.class, args);
    }
}
