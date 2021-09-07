package com.cyosp.ids.graphql.exception;

import java.util.Map;

public class IncorrectSizeException extends GraphQLErrorRuntimeException {
    private final int min;
    private final int max;

    public IncorrectSizeException(String message, int min, int max) {
        super(message);
        this.min = min;
        this.max = max;
    }

    @Override
    public Map<String, Object> getExtensions() {
        Map<String, Object> map = super.getExtensions();
        map.put("min", min);
        map.put("max", max);
        return map;
    }
}
