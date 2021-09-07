package com.cyosp.ids.repository;

import com.cyosp.ids.graphql.exception.IncorrectSizeException;
import com.cyosp.ids.model.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.Size;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.cyosp.ids.configuration.IdsConfiguration.DATA_DIRECTORY_PATH;
import static java.util.Objects.isNull;
import static org.springframework.beans.BeanUtils.copyProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepository {
    private final File usersFile = new File(DATA_DIRECTORY_PATH + "ids.users.json");

    private final ObjectMapper objectMapper;

    private final Validator validator;

    private List<User> users;

    @PostConstruct
    public void init() {
        if (usersFile.exists()) {
            try {
                users = objectMapper.readValue(usersFile, new TypeReference<>() {
                });
                setHashedPasswords();
            } catch (Exception e) {
                throw new RuntimeException("Fail to load users from: " + usersFile.getAbsolutePath() + "\n" + e.getMessage());
            }
        } else {
            users = new ArrayList<>();
            log.info("Users configuration file doesn't exist: " + usersFile.getAbsolutePath());
        }
    }

    private void setHashedPasswords() {
        AtomicBoolean needSaveUsers = new AtomicBoolean(false);
        users.forEach(user -> {
            if (isNull(user.getHashedPassword())) {
                user.setHashedPassword(user.getPassword());
                user.setPassword(null);
                needSaveUsers.set(true);
            }
        });
        if (needSaveUsers.get()) {
            saveUsers();
        }
    }

    public User save(User user) {
        validate(user);
        users.removeIf(u -> u.getId().equals(user.getId()));
        users.add(user);
        saveUsers();
        return user;
    }

    private void saveUsers() {
        try {
            objectMapper.writeValue(usersFile, users);
        } catch (Exception e) {
            throw new RuntimeException("Fail to save users into: " + usersFile.getAbsolutePath() + "\n" + e.getMessage());
        }
    }

    public List<User> findAll() {
        return users;
    }

    public User getByEmail(String email) {
        return users.stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst()
                .map(foundUser -> {
                    User user = new User();
                    copyProperties(foundUser, user);
                    return user;
                })
                .orElseThrow(() -> new UsernameNotFoundException("Email not found: " + email));
    }

    private void validate(User user) {
        Set<ConstraintViolation<User>> constraintViolations = validator.validate(user);
        if (!constraintViolations.isEmpty()) {
            constraintViolations.stream()
                    .findFirst()
                    .ifPresent(constraintViolation -> {
                        String annotation = constraintViolation.getConstraintDescriptor().getAnnotation().toString();
                        if (annotation.contains(Size.class.getName())) {
                            Map<String, Object> attributes = constraintViolation.getConstraintDescriptor().getAttributes();
                            int minSize = (int) attributes.get("min");
                            int maxSize = (int) attributes.get("max");
                            throw new IncorrectSizeException(constraintViolation.getMessage(), minSize, maxSize);
                        }
                    });
        }
    }
}
