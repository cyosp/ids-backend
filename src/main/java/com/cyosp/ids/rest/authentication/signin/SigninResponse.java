package com.cyosp.ids.rest.authentication.signin;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SigninResponse {
    private String accessToken;

    private String role;
}
