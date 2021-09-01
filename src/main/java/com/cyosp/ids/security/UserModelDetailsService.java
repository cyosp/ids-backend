package com.cyosp.ids.security;

import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import static java.util.List.of;

@Component
public class UserModelDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public UserModelDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(final String email) {
        return createSpringSecurityUser(userRepository.getByEmail(email));
    }

    private org.springframework.security.core.userdetails.User createSpringSecurityUser(User user) {
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getHashedPassword(),
                of(new SimpleGrantedAuthority(user.getRole().name())));
    }
}
