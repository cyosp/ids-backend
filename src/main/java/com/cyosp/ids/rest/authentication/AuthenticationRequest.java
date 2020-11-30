package com.cyosp.ids.rest.authentication;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class AuthenticationRequest {
    @Email
    @NotNull
    @Size(max = 50)
    private String email;

    @NotNull
    @Size(min = 8, max = 64)
    private String password;
}
