package com.cyosp.ids.graphql.exception;

import java.util.Map;

public class MediaDoesntExistException extends GraphQLErrorRuntimeException {
    private final String id;

    public MediaDoesntExistException(String id) {
        super("Media doesn't exist: " + id);
        this.id = id;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> map = super.getExtensions();
        map.put("id", id);
        return map;
    }
}
