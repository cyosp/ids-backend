package com.cyosp.ids.service;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

import static java.util.List.of;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

public class AuthenticationTestService {
    public void setAnonymousUser() {
        Collection<GrantedAuthority> grantedAuthorities = of(new SimpleGrantedAuthority("A_ROLE_VALUE"));
        User principal = new User("a_login", "a_password", grantedAuthorities);
        getContext().setAuthentication(new AnonymousAuthenticationToken("a_key", principal, grantedAuthorities));
    }

    public void setAuthenticatedUser(String login) {
        Collection<GrantedAuthority> grantedAuthorities = of(new SimpleGrantedAuthority("ROLE"));
        User principal = new User(login, "password", grantedAuthorities);
        getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities));
    }
}
