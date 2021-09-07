package com.cyosp.ids.graphql.exception;

public class BadCredentialsException extends GraphQLErrorRuntimeException {
    public BadCredentialsException(String message) {
        super(message);
    }
}
