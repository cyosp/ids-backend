package com.cyosp.ids.repository;

import com.cyosp.ids.model.TokenSetting;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;

import static com.cyosp.ids.configuration.IdsConfiguration.CONFIGURATION_DIRECTORY_PATH;

@Slf4j
@Component
public class TokenSettingRepository {
    private final File tokenSettingFile = new File(CONFIGURATION_DIRECTORY_PATH + "ids.token-setting.json");

    private final ObjectMapper objectMapper;

    private TokenSetting tokenSetting;

    TokenSettingRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (tokenSettingFile.exists()) {
            try {
                tokenSetting = objectMapper.readValue(tokenSettingFile, TokenSetting.class);
                log.info("Token settings loaded with success");
            } catch (Exception e) {
                throw new RuntimeException("Fail to load token settings from: " + tokenSettingFile.getAbsolutePath() + "\n" + e.getMessage());
            }
        } else {
            tokenSetting = save(new TokenSetting());
            log.info("Token settings file have been created: " + tokenSettingFile.getAbsolutePath());
        }
    }

    private TokenSetting save(TokenSetting tokenSetting) {
        try {
            objectMapper.writeValue(tokenSettingFile, tokenSetting);
        } catch (Exception e) {
            throw new RuntimeException("Fail to save token settings into: " + tokenSettingFile.getAbsolutePath() + "\n" + e.getMessage());
        }
        return tokenSetting;
    }

    public TokenSetting findOne() {
        return tokenSetting;
    }
}
