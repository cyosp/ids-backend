package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.FileSystemElement;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.cyosp.ids.model.Image.IDS_HIDDEN_DIRECTORY;
import static com.cyosp.ids.model.Role.ADMINISTRATOR;
import static java.io.File.separator;
import static java.lang.String.format;
import static java.util.Collections.reverse;
import static java.util.Objects.nonNull;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityService {
    private final IdsConfiguration idsConfiguration;

    @VisibleForTesting
    String getParent(String relativeFile) {
        int lastIndexOf = relativeFile.lastIndexOf(separator);
        return lastIndexOf == -1 ? null : relativeFile.substring(0, lastIndexOf);
    }

    @VisibleForTesting
    List<String> getDirectoryPaths(String relativeFile) {
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

    public boolean isAccessAllowed(String fileSystemElementId) {
        String login = getContext().getAuthentication().getName();
        log.info(format("[%s] Check access: %s", login, fileSystemElementId));
        for (String directoryPath : getDirectoryPaths(fileSystemElementId)) {
            File accessDeniedFile = new File(idsConfiguration.getAbsoluteImagesDirectory()
                    + separator + directoryPath
                    + separator + IDS_HIDDEN_DIRECTORY
                    + separator + "access.denied." + login);
            if (accessDeniedFile.exists()) {
                return false;
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
