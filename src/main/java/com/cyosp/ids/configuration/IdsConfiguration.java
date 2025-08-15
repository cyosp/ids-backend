package com.cyosp.ids.configuration;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separator;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getProperty;
import static java.util.Objects.isNull;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.tomlj.Toml.parse;

@Slf4j
@Component
public class IdsConfiguration {
    @VisibleForTesting
    static final String GENERAL_PASSWORD_CHANGE_ALLOWED = "general.password-change-allowed";
    @VisibleForTesting
    static final String MEDIAS_PUBLIC_SHARE_PROPERTY = "medias.public-share";
    @VisibleForTesting
    static final String MEDIAS_STATIC_PREVIEW_DIRECTORY_PROPERTY = "medias.static-preview-directory";
    @VisibleForTesting
    static final String SIGNUP_USER_PROPERTY = "signup.user";
    @VisibleForTesting
    static final String USERS_PASSWORD_CHANGE_DENIED = "users.password-change-denied";

    public static final String DATA_DIRECTORY_PATH = "data" + separator;
    private static final String CONFIGURATION_FILE_NAME = "ids.toml";

    private TomlParseResult tomlParseResult;

    @PostConstruct
    public void init() throws IOException {
        tomlParseResult = parse(Paths.get(DATA_DIRECTORY_PATH + CONFIGURATION_FILE_NAME));
        checkErrors();
    }

    void checkErrors() {
        List<TomlParseError> errors = tomlParseResult.errors();
        if (!errors.isEmpty()) {
            errors.forEach(error -> log.error(error.toString()));
            throw new RuntimeException("Fail to load configuration file: " + CONFIGURATION_FILE_NAME);
        }
    }

    @VisibleForTesting
    boolean isGeneralPasswordChangeAllowed() {
        Boolean generalPasswordChangeAllowed = tomlParseResult.getBoolean(GENERAL_PASSWORD_CHANGE_ALLOWED);
        return isNull(generalPasswordChangeAllowed) ? TRUE : TRUE.equals(generalPasswordChangeAllowed);
    }

    public boolean isPasswordChangeAllowed() {
        if (isGeneralPasswordChangeAllowed()) {
            TomlArray usersPasswordChangeDeniedTomlArray = tomlParseResult.getArray(USERS_PASSWORD_CHANGE_DENIED);
            List<Object> usersPasswordChangeDenied = isNull(usersPasswordChangeDeniedTomlArray) ? new ArrayList<>() : usersPasswordChangeDeniedTomlArray.toList();
            return usersPasswordChangeDenied.stream()
                    .map(String.class::cast)
                    .noneMatch(login -> login.equals(getContext().getAuthentication().getName()));
        } else {
            return false;
        }
    }

    public String getAbsoluteMediasDirectory() {
        String imagesDirectory = tomlParseResult.getString("medias.directory");
        if (!imagesDirectory.startsWith(separator)) {
            String userDir = getProperty("user.dir");
            if (!userDir.endsWith(separator)) userDir += separator;
            imagesDirectory = userDir + imagesDirectory;
        }
        return imagesDirectory;
    }

    public boolean userCanSignup() {
        Boolean userCanSignup = tomlParseResult.getBoolean(SIGNUP_USER_PROPERTY);
        return isNull(userCanSignup) ? TRUE : TRUE.equals(userCanSignup);
    }

    public boolean areMediasPublicShared() {
        return TRUE.equals(tomlParseResult.getBoolean(MEDIAS_PUBLIC_SHARE_PROPERTY));
    }

    public boolean isStaticPreviewDirectory() {
        return TRUE.equals(tomlParseResult.getBoolean(MEDIAS_STATIC_PREVIEW_DIRECTORY_PROPERTY));
    }
}
