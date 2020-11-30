package com.cyosp.ids.rest.authentication.signin;

import com.cyosp.ids.rest.authentication.AuthenticationRequest;
import com.cyosp.ids.security.jwt.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.cyosp.ids.rest.authentication.signin.SigninController.SIGNIN_PATH;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@RestController
@RequestMapping(SIGNIN_PATH)
public class SigninController {
    public static final String SIGNIN_PATH = "/api/auth/signin";

    private final JwtTokenProvider jwtTokenProvider;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    public SigninController(JwtTokenProvider jwtTokenProvider, AuthenticationManagerBuilder authenticationManagerBuilder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
    }

    @PostMapping
    public ResponseEntity<SigninResponse> authorize(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(authenticationRequest.getEmail(), authenticationRequest.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        getContext().setAuthentication(authentication);

        HttpHeaders httpHeaders = new HttpHeaders();
        String jwt = jwtTokenProvider.createToken(authentication);
        httpHeaders.add("Authorization", "Bearer " + jwt);

        return new ResponseEntity<>(new SigninResponse(jwt), httpHeaders, OK);
    }
}
