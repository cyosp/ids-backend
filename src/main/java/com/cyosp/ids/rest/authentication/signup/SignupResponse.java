package com.cyosp.ids.rest.authentication.signup;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class SignupResponse {
    private String id;
}
