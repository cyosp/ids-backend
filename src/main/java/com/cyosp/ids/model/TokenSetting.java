package com.cyosp.ids.model;

import lombok.Data;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;

@Data
public class TokenSetting {
    private static final long ONE_HOUR_IN_SECONDS = 3600;
    private static final long ONE_SECOND_IN_MILLISECONDS = 1000;

    private final byte[] key;

    private final long duration;

    public TokenSetting() {
        // Key size >= 32 characters to respect: keys used with HMAC-SHA algorithms MUST have a size >= 256 bits
        key = randomAlphanumeric(64).getBytes();

        duration = ONE_HOUR_IN_SECONDS * ONE_SECOND_IN_MILLISECONDS;
    }
}
