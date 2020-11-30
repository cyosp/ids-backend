package com.cyosp.ids.model;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Role {
    ADMINISTRATOR("administrator"),
    VIEWER("viewer");

    private final String value;

    @JsonValue
    public String getValue() {
        return value;
    }
}
