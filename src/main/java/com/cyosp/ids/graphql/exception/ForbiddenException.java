package com.cyosp.ids.graphql.exception;

public class ForbiddenException extends GraphQLErrorRuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
