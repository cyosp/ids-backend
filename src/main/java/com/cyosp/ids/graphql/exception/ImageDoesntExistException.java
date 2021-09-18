package com.cyosp.ids.graphql.exception;

import java.util.Map;

public class ImageDoesntExistException extends GraphQLErrorRuntimeException {
    private final String id;

    public ImageDoesntExistException(String id) {
        super("Image doesn't exist: " + id);
        this.id = id;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> map = super.getExtensions();
        map.put("id", id);
        return map;
    }
}
