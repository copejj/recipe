package com.braindribbler.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // <-- Add import
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public AuthenticationEventPublisher authenticationEventPublisher(
                        ApplicationEventPublisher applicationEventPublisher) {
                // Forces Spring to broadcast hidden exceptions (like DisabledException) to your
                // EventListener
                return new DefaultAuthenticationEventPublisher(applicationEventPublisher);
        }

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

                                                // Broad administrative catch-alls (Hybrid baseline mapping protection)
                                                .requestMatchers("/admin/users/**").hasAuthority("can_manage_users")
                                                .requestMatchers("/admin/roles/**").hasAuthority("can_manage_users")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureHandler((request, response, exception) -> {
                                                        if (exception instanceof LockedException) {
                                                                response.sendRedirect("/login?iplocked");
                                                        } else if (exception instanceof DisabledException) {
                                                                // Natively catches the exception and routes to your
                                                                // Inactive banner!
                                                                response.sendRedirect("/login?inactive");
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
