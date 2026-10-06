package com.braindribbler.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 16384, 2);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // CRITICAL: Permit /error so 404s/500s do not trigger a login redirect loop
                        .requestMatchers("/error").permitAll()
                        // Permit static assets and our test pages
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/", "/test-home", "/test-target", "/recipes/search").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        // Spring Security's default processing endpoint
                        .permitAll()
                        .defaultSuccessUrl("/test-home", true))
                .logout(logout -> logout
                        .logoutSuccessUrl("/test-home")
                        .permitAll());

        return http.build();
    }
}
