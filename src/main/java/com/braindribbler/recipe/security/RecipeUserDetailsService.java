package com.braindribbler.recipe.security;

import com.braindribbler.recipe.repository.auth.UserAuthRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RecipeUserDetailsService implements UserDetailsService {

    private final UserAuthRepository userAuthRepository;

    public RecipeUserDetailsService(UserAuthRepository userAuthRepository) {
        this.userAuthRepository = userAuthRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userAuthRepository.findByEmail(email)
                .map(RecipeUserDetails::new)
                .orElseThrow(
                        () -> new UsernameNotFoundException("No active account discovered matching email: " + email));
    }
}
