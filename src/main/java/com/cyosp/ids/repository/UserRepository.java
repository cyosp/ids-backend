package com.cyosp.ids.repository;

import com.cyosp.ids.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.cyosp.ids.configuration.IdsConfiguration.CONFIGURATION_DIRECTORY_PATH;

@Slf4j
@Component
public class UserRepository {
    private final File usersFile = new File(CONFIGURATION_DIRECTORY_PATH + "ids.users.json");

    private final ObjectMapper objectMapper;

    private List<User> users;

    UserRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        if (usersFile.exists()) {
            try {
                users = objectMapper.readValue(usersFile, new TypeReference<>() {
                });
            } catch (Exception e) {
                throw new RuntimeException("Fail to load users from: " + usersFile.getAbsolutePath() + "\n" + e.getMessage());
            }
        } else {
            users = new ArrayList<>();
            log.info("Users configuration file doesn't exist: " + usersFile.getAbsolutePath());
        }
    }

    public User save(User user) {
        users.add(user);
        try {
            objectMapper.writeValue(usersFile, users);
        } catch (Exception e) {
            throw new RuntimeException("Fail to save users into: " + usersFile.getAbsolutePath() + "\n" + e.getMessage());
        }
        return user;
    }

    public List<User> findAll() {
        return users;
    }

    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst();
    }
}
