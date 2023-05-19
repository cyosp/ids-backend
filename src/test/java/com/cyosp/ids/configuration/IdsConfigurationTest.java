package com.cyosp.ids.configuration;

import com.cyosp.ids.service.AuthenticationTestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseResult;

import java.util.List;

import static com.cyosp.ids.configuration.IdsConfiguration.GENERAL_PASSWORD_CHANGE_ALLOWED;
import static com.cyosp.ids.configuration.IdsConfiguration.IMAGES_PUBLIC_SHARE_PROPERTY;
import static com.cyosp.ids.configuration.IdsConfiguration.IMAGES_STATIC_PREVIEW_DIRECTORY_PROPERTY;
import static com.cyosp.ids.configuration.IdsConfiguration.SIGNUP_USER_PROPERTY;
import static com.cyosp.ids.configuration.IdsConfiguration.USERS_PASSWORD_CHANGE_DENIED;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class IdsConfigurationTest {
    @Mock
    private TomlParseResult tomlParseResult;

    @InjectMocks
    private IdsConfiguration idsConfiguration;

    private AuthenticationTestService authenticationTestService;

    @BeforeEach
    void beforeEach() {
        idsConfiguration = spy(idsConfiguration);
        authenticationTestService = new AuthenticationTestService();
    }

    @Test
    void isGeneralPasswordChangeAllowed_propertyNotDefined() {
        doReturn(null)
                .when(tomlParseResult)
                .getBoolean(GENERAL_PASSWORD_CHANGE_ALLOWED);

        assertTrue(idsConfiguration.isGeneralPasswordChangeAllowed());
    }

    @Test
    void isGeneralPasswordChangeAllowed_propertySetToTrue() {
        doReturn(true)
                .when(tomlParseResult)
                .getBoolean(GENERAL_PASSWORD_CHANGE_ALLOWED);

        assertTrue(idsConfiguration.isGeneralPasswordChangeAllowed());
    }

    @Test
    void isGeneralPasswordChangeAllowed_propertySetToFalse() {
        doReturn(false)
                .when(tomlParseResult)
                .getBoolean(GENERAL_PASSWORD_CHANGE_ALLOWED);

        assertFalse(idsConfiguration.isGeneralPasswordChangeAllowed());
    }

    @Test
    void isPasswordChangeAllowed_isGeneralPasswordChangeAllowedIsFalse() {
        doReturn(false)
                .when(idsConfiguration)
                .isGeneralPasswordChangeAllowed();

        assertFalse(idsConfiguration.isPasswordChangeAllowed());
    }

    @Test
    void isPasswordChangeAllowed_isGeneralPasswordChangeAllowedIsTrue_propertyNotDefined() {
        doReturn(true)
                .when(idsConfiguration)
                .isGeneralPasswordChangeAllowed();

        doReturn(null)
                .when(tomlParseResult)
                .getArray(USERS_PASSWORD_CHANGE_DENIED);

        assertTrue(idsConfiguration.isPasswordChangeAllowed());
    }

    @Test
    void isPasswordChangeAllowed_isGeneralPasswordChangeAllowedIsTrue_userNotInList() {
        authenticationTestService.setAuthenticatedUser("an@authenticated.user");

        doReturn(true)
                .when(idsConfiguration)
                .isGeneralPasswordChangeAllowed();

        TomlArray tomlArray = mock(TomlArray.class);
        doReturn(tomlArray)
                .when(tomlParseResult)
                .getArray(USERS_PASSWORD_CHANGE_DENIED);

        doReturn(of("a@user.ids"))
                .when(tomlArray)
                .toList();

        assertTrue(idsConfiguration.isPasswordChangeAllowed());
    }

    @Test
    void isPasswordChangeAllowed_isGeneralPasswordChangeAllowedIsTrue_userDenied() {
        String login = "a@denied.user";
        authenticationTestService.setAuthenticatedUser(login);

        doReturn(true)
                .when(idsConfiguration)
                .isGeneralPasswordChangeAllowed();

        TomlArray tomlArray = mock(TomlArray.class);
        doReturn(tomlArray)
                .when(tomlParseResult)
                .getArray(USERS_PASSWORD_CHANGE_DENIED);

        doReturn(of(login))
                .when(tomlArray)
                .toList();

        assertFalse(idsConfiguration.isPasswordChangeAllowed());
    }

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

    @Test
    void areImagesPublicShared_propertyNotDefined() {
        doReturn(null)
                .when(tomlParseResult)
                .getBoolean(IMAGES_PUBLIC_SHARE_PROPERTY);

        assertFalse(idsConfiguration.areImagesPublicShared());
    }

    @Test
    void areImagesPublicShared_propertySetToTrue() {
        doReturn(true)
                .when(tomlParseResult)
                .getBoolean(IMAGES_PUBLIC_SHARE_PROPERTY);

        assertTrue(idsConfiguration.areImagesPublicShared());
    }

    @Test
    void areImagesPublicShared_propertySetToFalse() {
        doReturn(false)
                .when(tomlParseResult)
                .getBoolean(IMAGES_PUBLIC_SHARE_PROPERTY);

        assertFalse(idsConfiguration.areImagesPublicShared());
    }

    @Test
    void isStaticPreviewDirectory_propertyNotDefined() {
        doReturn(null)
                .when(tomlParseResult)
                .getBoolean(IMAGES_STATIC_PREVIEW_DIRECTORY_PROPERTY);

        assertFalse(idsConfiguration.isStaticPreviewDirectory());
    }

    @Test
    void isStaticPreviewDirectory_propertySetToTrue() {
        doReturn(true)
                .when(tomlParseResult)
                .getBoolean(IMAGES_STATIC_PREVIEW_DIRECTORY_PROPERTY);

        assertTrue(idsConfiguration.isStaticPreviewDirectory());
    }

    @Test
    void isStaticPreviewDirectory_propertySetToFalse() {
        doReturn(false)
                .when(tomlParseResult)
                .getBoolean(IMAGES_STATIC_PREVIEW_DIRECTORY_PROPERTY);

        assertFalse(idsConfiguration.isStaticPreviewDirectory());
    }
}
