package com.cyosp.ids.rest.authentication.signin;

import com.cyosp.ids.model.Role;
import com.cyosp.ids.rest.authentication.AuthenticationRequest;
import com.cyosp.ids.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.cyosp.ids.model.Role.ADMINISTRATOR;
import static com.cyosp.ids.model.Role.VIEWER;
import static com.cyosp.ids.rest.authentication.signin.SigninController.SIGNIN_PATH;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SIGNIN_PATH)
public class SigninController {
    public static final String SIGNIN_PATH = "/api/auth/signin";

    private final JwtTokenProvider jwtTokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final LoggedService loggedService;

    @PostMapping
    public ResponseEntity<SigninResponse> authorize(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        getContext().setAuthentication(authentication);

        loggedService.add(authenticationRequest.getEmail());

        return new ResponseEntity<>(new SigninResponse(
                jwtTokenProvider.createToken(authentication),
                authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> ADMINISTRATOR.name().equals(authority))
                        .map(Role::valueOf)
                        .findFirst()
                        .orElse(VIEWER)
                        .name()),
                new HttpHeaders(),
                OK);
    }
}
