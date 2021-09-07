package com.cyosp.ids.graphql.exception;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public abstract class GraphQLErrorRuntimeException extends RuntimeException implements GraphQLError {
    protected GraphQLErrorRuntimeException(String message) {
        super(message);
    }

    @Override
    public List<SourceLocation> getLocations() {
        return emptyList();
    }

    @Override
    public ErrorClassification getErrorType() {
        return null;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> map = new HashMap<>();
        map.put("exceptionName", this.getClass().getSimpleName());
        return map;
    }
}
