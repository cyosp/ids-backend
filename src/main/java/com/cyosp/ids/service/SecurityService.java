package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Group;
import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static com.cyosp.ids.model.Directory.IDS_HIDDEN_DIRECTORY;
import static com.cyosp.ids.model.Role.ADMINISTRATOR;
import static com.cyosp.ids.model.Role.GUEST;
import static java.io.File.separator;
import static java.lang.String.format;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.list;
import static java.nio.file.Paths.get;
import static java.util.Collections.reverse;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {
    private final IdsConfiguration idsConfiguration;
    private final UserRepository userRepository;
    private final ModelService modelService;

    @VisibleForTesting
    boolean hasAuthentication() {
        return nonNull(getContext().getAuthentication());
    }

    @VisibleForTesting
    boolean isAnonymousUser() {
        return getContext().getAuthentication() instanceof AnonymousAuthenticationToken;
    }

    public boolean isGuestUser() {
        return getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> GUEST.name().equals(authority));
    }

    @VisibleForTesting
    boolean needAccessCheck() {
        return hasAuthentication() && !isAnonymousUser();
    }

    @VisibleForTesting
    String getParent(String relativeFile) {
        int lastIndexOf = relativeFile.lastIndexOf(separator);
        return lastIndexOf == -1 ? null : relativeFile.substring(0, lastIndexOf);
    }

    @VisibleForTesting
    List<String> getPaths(String relativeFile) {
        String directory = nonNull(relativeFile) ? relativeFile : "";

        List<String> directories = new ArrayList<>();
        directories.add(directory);
        String parent = getParent(directory);
        while (nonNull(parent)) {
            directories.add(parent);
            parent = getParent(parent);
        }
        reverse(directories);
        return directories;
    }

    private Set<String> listFiles(String dir) {
        try (Stream<Path> stream = list(get(dir))) {
            return stream
                    .filter(file -> !isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(toSet());
        } catch (NoSuchFileException e) { // Case where .ids directory doesn't exist for example
            return Set.of();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isAccessAllowed(String fileSystemElementId) {
        if (needAccessCheck()) {
            String login = getContext().getAuthentication().getName();
            String logPrefix = format("[%s] Check access: %s, ", login, fileSystemElementId);

            User user = userRepository.getByEmail(login);
            if (isGuestUser()) {
                // Guest user can't access to root directory, otherwise it means gallery is opened to everybody
                // He can only access starting its home directory
                boolean accessAllowed = nonNull(fileSystemElementId) && fileSystemElementId.startsWith(user.getHome());
                if (!accessAllowed) {
                    log.warn(logPrefix + "Guest not allowed");
                }
                return accessAllowed;
            }

            String accessLimitedPrefix = "access.limited.";
            List<Group> userGroups = user.getGroups();

            for (String path : getPaths(fileSystemElementId)) {
                String absolutePath = idsConfiguration.getAbsoluteMediasDirectory()
                        + (isBlank(path) ? "" : separator) + path;
                if (modelService.isDirectory(Path.of(absolutePath))) {
                    String directory = absolutePath
                            + separator + IDS_HIDDEN_DIRECTORY;
                    Set<String> files = listFiles(directory);

                    List<String> limitedAccessFiles = files.stream()
                            .filter(fileName -> fileName.startsWith(accessLimitedPrefix))
                            .toList();
                    // When there is an access limited file present, access is limited to the user login or through one of its group names
                    // Otherwise access is forbidden
                    if (!limitedAccessFiles.isEmpty()
                            && !limitedAccessFiles.contains(accessLimitedPrefix + "user.name." + login)
                            && (userGroups.isEmpty() || userGroups.stream()
                            .noneMatch(group -> limitedAccessFiles.contains(accessLimitedPrefix + "group.name." + group.getName())))) {
                        log.warn(logPrefix + "Not allowed due to limited access");
                        return false;
                    }

                    // TODO Add group management for denied access
                    if (!files.isEmpty() && files.stream()
                            .anyMatch(fileName -> fileName.equals("access.denied." + login))) {
                        log.warn(logPrefix + "Not allowed due to denied access");
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public boolean isAccessAllowed(FileSystemElement fileSystemElement) {
        return isAccessAllowed(fileSystemElement.getId());
    }

    public void checkAccessAllowed(String fileSystemElementId) {
        if (!isAccessAllowed(fileSystemElementId)) {
            String message = "Access denied";
            log.info(format("[%s] %s", getContext().getAuthentication().getName(), message));
            throw new AccessDeniedException(message);
        }
    }

    public void checkAccessAllowed(FileSystemElement fileSystemElement) {
        checkAccessAllowed(fileSystemElement.getId());
    }

    public void checkAdministratorUser() throws java.nio.file.AccessDeniedException {
        if (getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(authority -> ADMINISTRATOR.name().equals(authority)))
            throw new java.nio.file.AccessDeniedException("Only administrator user is allowed");
    }
}
