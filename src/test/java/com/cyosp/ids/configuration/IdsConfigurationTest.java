package com.cyosp.ids.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tomlj.TomlParseResult;

import static com.cyosp.ids.configuration.IdsConfiguration.SIGNUP_USER_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class IdsConfigurationTest {
    @Mock
    private TomlParseResult tomlParseResult;

    @InjectMocks
    private IdsConfiguration idsConfiguration;

    @Test
    void userCanSignup_propertyNotDefined() {
        doReturn(null)
                .when(tomlParseResult)
                .getBoolean(SIGNUP_USER_PROPERTY);

        assertTrue(idsConfiguration.userCanSignup());
    }

    @Test
    void userCanSignup_propertySetToTrue() {
        doReturn(true)
                .when(tomlParseResult)
                .getBoolean(SIGNUP_USER_PROPERTY);

        assertTrue(idsConfiguration.userCanSignup());
    }

    @Test
    void userCanSignup_propertySetToFalse() {
        doReturn(false)
                .when(tomlParseResult)
                .getBoolean(SIGNUP_USER_PROPERTY);

        assertFalse(idsConfiguration.userCanSignup());
    }
}