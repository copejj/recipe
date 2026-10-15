package com.braindribbler.recipe.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import com.braindribbler.recipe.security.exception.AuthenticationExceptions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
        @Bean
        public AuthenticationEventPublisher authenticationEventPublisher(
                        ApplicationEventPublisher applicationEventPublisher) {

                DefaultAuthenticationEventPublisher publisher = new DefaultAuthenticationEventPublisher(
                                applicationEventPublisher);

                // Define the modern, type-safe exception-to-event mappings
                java.util.Map<Class<? extends org.springframework.security.core.AuthenticationException>, Class<? extends org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent>> mappings = new java.util.HashMap<>();

                mappings.put(AuthenticationExceptions.AccountInactiveException.class,
                                org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent.class);

                mappings.put(AuthenticationExceptions.AccountUnverifiedException.class,
                                org.springframework.security.authentication.event.AuthenticationFailureDisabledEvent.class);

                mappings.put(AuthenticationExceptions.IpBruteForceLockedException.class,
                                org.springframework.security.authentication.event.AuthenticationFailureLockedEvent.class);

                publisher.setAdditionalExceptionMappings(mappings);

                return publisher;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        PasswordEncoder passwordEncoder,
                        UserDetailsService userDetailsService) throws Exception {

                DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
                authProvider.setPasswordEncoder(passwordEncoder);
                authProvider.setHideUserNotFoundExceptions(false);

                // --- NEW: Handle raw token values when working with cookie repositories ---
                org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler requestHandler = new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler();
                // By default, Spring Security 6 uses a XORed token handler which causes
                // mismatches with raw cookies
                requestHandler.setCsrfRequestAttributeName(null);

                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(requestHandler)) // ◄ FIXES COOKIE TOKEN
                                                                                          // MISMATCH

                                // --- FIXED: Explicit type assignment ensures seamless compilation ---
                                .addFilterAfter((jakarta.servlet.Filter) (request, response, chain) -> {
                                        org.springframework.security.web.csrf.CsrfToken csrfToken = (org.springframework.security.web.csrf.CsrfToken) request
                                                        .getAttribute(org.springframework.security.web.csrf.CsrfToken.class
                                                                        .getName());
                                        if (csrfToken != null) {
                                                csrfToken.getToken(); // Forces immediate cookie synchronization
                                        }
                                        chain.doFilter(request, response);
                                }, org.springframework.security.web.csrf.CsrfFilter.class)

                                .authenticationProvider(authProvider)
                                .authorizeHttpRequests(auth -> auth
                                                .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.FORWARD)
                                                .permitAll()
                                                .requestMatchers("/error").permitAll()
                                                .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**")
                                                .permitAll()
                                                .requestMatchers("/", "/search", "/register", "/login", "/verify",
                                                                "/forgot-password", "/reset-password")
                                                .permitAll()
                                                .requestMatchers("/admin/users/**").hasAuthority("can_manage_users")
                                                .requestMatchers("/admin/roles/**").hasAuthority("can_manage_users")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/login")
                                                .defaultSuccessUrl("/dashboard", true)
                                                .failureHandler((request, response, exception) -> {
                                                        if (exception instanceof AuthenticationExceptions.IpBruteForceLockedException) {
                                                                response.sendRedirect("/login?iplocked");
                                                        } else if (exception instanceof AuthenticationExceptions.AccountInactiveException) {
                                                                response.sendRedirect("/login?inactive");
                                                        } else if (exception instanceof AuthenticationExceptions.AccountUnverifiedException) {
                                                                response.sendRedirect("/login?unverified");
                                                        } else if (exception instanceof BadCredentialsException) {
                                                                response.sendRedirect("/login?error");
                                                        } else {
                                                                response.sendRedirect("/login?systemError");
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
