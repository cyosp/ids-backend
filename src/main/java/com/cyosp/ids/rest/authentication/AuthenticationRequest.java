package com.cyosp.ids.rest.authentication;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import static com.cyosp.ids.model.User.PASSWORD_MAX_LENGTH;
import static com.cyosp.ids.model.User.PASSWORD_MIN_LENGTH;

@Data
public class AuthenticationRequest {
    @Email
    @NotNull
    @Size(max = 50)
    private String email;

    @NotNull
    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    private String password;
}
