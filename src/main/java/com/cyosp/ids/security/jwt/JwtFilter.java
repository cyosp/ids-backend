package com.cyosp.ids.security.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.util.StringUtils.hasText;

@Slf4j
public class JwtFilter extends GenericFilterBean {
    private final JwtTokenProvider jwtTokenProvider;

    public JwtFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwt = extractToken(httpServletRequest);
        String requestURI = httpServletRequest.getRequestURI();

        if (hasText(jwt)) {
            if (jwtTokenProvider.isValid(jwt)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
                getContext().setAuthentication(authentication);
                log.info("Set authentication to security context for {} and uri: {}", authentication.getName(), requestURI);
            } else {
                log.info("No valid JWT token found for uri: {}", requestURI);
            }
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String extractToken(HttpServletRequest httpServletRequest) {
        String authorization = httpServletRequest.getHeader("Authorization");
        if (hasText(authorization) && authorization.startsWith("Bearer "))
            return authorization.substring(7);
        else
            return null;
    }
}
