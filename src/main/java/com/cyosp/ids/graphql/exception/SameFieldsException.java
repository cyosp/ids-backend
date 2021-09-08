package com.cyosp.ids.graphql.exception;

public class SameFieldsException extends GraphQLErrorRuntimeException {
    public SameFieldsException(String message) {
        super(message);
    }
}
