package com.prepcreatine.security;

import com.prepcreatine.domain.User;
import com.prepcreatine.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads UserDetails by email address for Spring Security authentication.
 */
@Service
public class PrepCreatineUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public PrepCreatineUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new PrepCreatineUserDetails(user);
    }
}
