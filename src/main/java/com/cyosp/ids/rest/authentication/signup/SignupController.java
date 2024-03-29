package com.cyosp.ids.rest.authentication.signup;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Role;
import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
import com.cyosp.ids.rest.authentication.AuthenticationRequest;
import com.cyosp.ids.service.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

import static com.cyosp.ids.model.Role.ADMINISTRATOR;
import static com.cyosp.ids.model.Role.VIEWER;
import static com.cyosp.ids.rest.authentication.signup.SignupController.SIGNUP_PATH;
import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(SIGNUP_PATH)
public class SignupController {
    public static final String SIGNUP_PATH = "/api/auth/signup";

    private final UserRepository userRepository;

    private final IdsConfiguration idsConfiguration;

    private final PasswordService passwordService;

    ResponseEntity<SignupResponse> register(AuthenticationRequest authenticationRequest, Role role) {
        String email = authenticationRequest.getEmail();

        boolean userFound = userRepository.findAll().stream()
                .anyMatch(user -> email.equals(user.getEmail()));
        if (userFound)
            throw new AuthenticationServiceException("User already registered");

        String password = authenticationRequest.getPassword();
        User user = User.builder()
                .id(randomUUID().toString())
                .email(authenticationRequest.getEmail())
                .password(password)
                .hashedPassword(passwordService.encode(password))
                .role(role)
                .build();

        User savedUser = userRepository.save(user);

        return new ResponseEntity<>(SignupResponse.builder()
                .id(savedUser.getId())
                .build(), OK);
    }

    @PostMapping("/admin")
    public ResponseEntity<SignupResponse> adminRegister(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        boolean adminUserFound = userRepository.findAll().stream()
                .anyMatch(user -> user.getRole() == ADMINISTRATOR);
        if (adminUserFound) {
            throw new AuthenticationServiceException("Administrator user already registered");
        } else
            return register(authenticationRequest, ADMINISTRATOR);
    }

    @PostMapping
    public ResponseEntity<SignupResponse> userRegister(@Valid @RequestBody AuthenticationRequest authenticationRequest) {
        if (idsConfiguration.userCanSignup()) {
            return register(authenticationRequest, VIEWER);
        } else {
            throw new AuthenticationServiceException("Register user not allowed");
        }
    }
}
