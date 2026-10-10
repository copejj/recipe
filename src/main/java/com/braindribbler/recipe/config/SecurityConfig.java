package com.braindribbler.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        PasswordEncoder passwordEncoder,
                        UserDetailsService userDetailsService) throws Exception {

                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);

                authProvider.setHideUserNotFoundExceptions(false);

                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                                .authenticationProvider(authProvider)
                                .authorizeHttpRequests(auth -> auth
                                                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD)
                                                .permitAll()

                                                // Public endpoints and assets
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/", "/search", "/register", "/login", "/verify",
                                                                "/error")
                                                .permitAll()
                                                .requestMatchers("/admin/users/**").hasAuthority("can_manage_users")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureHandler((request, response, exception) -> {

                                                        Throwable cause = (exception.getCause() != null)
                                                                        ? exception.getCause()
                                                                        : exception;

                                                        String errorMessage = cause.getMessage();

                                                        if (cause instanceof LockedException) {
                                                                response.sendRedirect("/login?iplocked");
                                                        } else if (cause instanceof DisabledException) {
                                                                if (errorMessage != null && errorMessage.contains(
                                                                                "administratively disabled")) {
                                                                        response.sendRedirect("/login?banned");
                                                                } else {
                                                                        response.sendRedirect("/login?unverified");
                                                                }
                                                        } else {
                                                                response.sendRedirect("/login?error");
                                                        }
                                                })
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout")
                                                .invalidateHttpSession(true)
                                                .clearAuthentication(true)
                                                .deleteCookies("RECIPE_JSESSIONID", "JSESSIONID", "XSRF-TOKEN")
                                                .permitAll());
                return http.build();
        }
}
