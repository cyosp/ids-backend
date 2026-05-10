package com.cyosp.ids.rest.authentication.signin;

import com.cyosp.ids.model.Role;
import com.cyosp.ids.repository.UserRepository;
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

import java.util.List;

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
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SigninResponse> authorize(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        String email = authenticationRequest.getEmail();
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(email, authenticationRequest.getPassword());

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        getContext().setAuthentication(authentication);

        loggedService.add(email);

       List<Role> roles =  authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(Role::valueOf)
                .toList();

        return new ResponseEntity<>(new SigninResponse(
                jwtTokenProvider.createToken(authentication),
                (roles.contains(ADMINISTRATOR) ? ADMINISTRATOR : roles.getFirst()).name(),
                userRepository.getByEmail(email).getHome()),
                new HttpHeaders(),
                OK);
    }
}
