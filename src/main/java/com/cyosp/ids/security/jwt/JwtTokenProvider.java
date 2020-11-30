package com.cyosp.ids.security.jwt;

import com.cyosp.ids.model.TokenSetting;
import com.cyosp.ids.repository.TokenSettingRepository;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import static io.jsonwebtoken.SignatureAlgorithm.HS512;
import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

@Slf4j
@Component
public class JwtTokenProvider {
    private static final String AUTHORITIES_KEY = "auth";
    private static final String AUTHORITIES_SEPARATOR = ",";

    private final TokenSettingRepository tokenSettingRepository;

    private Key key;
    private long duration;

    public JwtTokenProvider(TokenSettingRepository tokenSettingRepository) {
        this.tokenSettingRepository = tokenSettingRepository;
    }

    @PostConstruct
    public void init() {
        TokenSetting tokenSetting = tokenSettingRepository.findOne();
        key = hmacShaKeyFor(tokenSetting.getKey());
        duration = tokenSetting.getDuration();
    }

    public String createToken(Authentication authentication) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(joining(AUTHORITIES_SEPARATOR));

        long now = (new Date()).getTime();
        Date endValidityDate = new Date(now + duration);

        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .signWith(key, HS512)
                .setExpiration(endValidityDate)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(AUTHORITIES_SEPARATOR))
                        .map(SimpleGrantedAuthority::new)
                        .collect(toList());

        User principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    public boolean isValid(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature");
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token");
        } catch (IllegalArgumentException e) {
            log.info("Invalid JWT token");
        }
        return false;
    }
}
