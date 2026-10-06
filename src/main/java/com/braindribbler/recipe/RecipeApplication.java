package com.braindribbler.recipe;

import com.braindribbler.recipe.config.SecurityConfig; // Import your custom security profile configuration
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;

@SpringBootApplication
@PropertySource(value = "classpath:secrets.properties", ignoreResourceNotFound = true)
public class RecipeApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        // Explicitly register SecurityConfig here so embedded or external servlet
        // containers scan it
        return application.sources(RecipeApplication.class, SecurityConfig.class);
    }

    public static void main(String[] args) {
        // Pass both classes into the standalone runner context
        SpringApplication.run(new Class<?>[] { RecipeApplication.class, SecurityConfig.class }, args);
    }
}
