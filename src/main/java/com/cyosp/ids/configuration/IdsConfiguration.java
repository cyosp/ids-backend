package com.cyosp.ids.configuration;

import com.google.common.annotations.VisibleForTesting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static java.io.File.separator;
import static java.lang.Boolean.TRUE;
import static java.lang.System.getProperty;
import static java.util.Objects.isNull;
import static org.tomlj.Toml.parse;

@Slf4j
@Component
public class IdsConfiguration {
    @VisibleForTesting
    static final String GENERAL_PASSWORD_CHANGE_ALLOWED = "general.password-change-allowed";
    @VisibleForTesting
    static final String IMAGES_PUBLIC_SHARE_PROPERTY = "images.public-share";
    @VisibleForTesting
    static final String IMAGES_STATIC_PREVIEW_DIRECTORY_PROPERTY = "images.static-preview-directory";
    @VisibleForTesting
    static final String SIGNUP_USER_PROPERTY = "signup.user";

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

    public boolean isPasswordChangeAllowed() {
        Boolean generalPasswordChangeAllowed = tomlParseResult.getBoolean(GENERAL_PASSWORD_CHANGE_ALLOWED);
        return isNull(generalPasswordChangeAllowed) ? TRUE : TRUE.equals(generalPasswordChangeAllowed);
    }

    public String getAbsoluteImagesDirectory() {
        String imagesDirectory = tomlParseResult.getString("images.directory");
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

    public boolean areImagesPublicShared() {
        return TRUE.equals(tomlParseResult.getBoolean(IMAGES_PUBLIC_SHARE_PROPERTY));
    }

    public boolean isStaticPreviewDirectory() {
        return TRUE.equals(tomlParseResult.getBoolean(IMAGES_STATIC_PREVIEW_DIRECTORY_PROPERTY));
    }
}
