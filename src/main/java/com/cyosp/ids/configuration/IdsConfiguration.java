package com.cyosp.ids.configuration;

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
        final Boolean DEFAULT_SIGNUP_USER_BEHAVIOR = TRUE;
        Boolean userCanSignup = tomlParseResult.getBoolean("signup.user");
        if (isNull(userCanSignup)) {
            userCanSignup = DEFAULT_SIGNUP_USER_BEHAVIOR;
        }
        return TRUE.equals(userCanSignup);
    }
}
