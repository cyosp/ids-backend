package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Directory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static java.io.File.separator;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.List.of;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Mock
    private IdsConfiguration idsConfiguration;

    private SecurityService securityService;

    private File temporaryBaseDirectory;

    @BeforeEach
    void beforeEach() {
        securityService = spy(new SecurityService(idsConfiguration));
    }

    @AfterEach
    void afterEach() {
        if (nonNull(temporaryBaseDirectory) && temporaryBaseDirectory.exists()) {
            if (!deleteRecursively(temporaryBaseDirectory)) {
                throw new RuntimeException("Fail to delete directory: " + temporaryBaseDirectory);
            }
        }
    }

    @Test
    void getParent() {
        assertEquals("/a/b", securityService.getParent("/a/b/c"));
    }

    @Test
    void getDirectoryPaths() {
        List<String> directories = securityService.getDirectoryPaths("a/b/c");

        assertEquals(asList("a", "a/b", "a/b/c"), directories);
    }

    private void setAuthentication(String login) {
        Collection<GrantedAuthority> grantedAuthorities = of(new SimpleGrantedAuthority("ROLE"));
        User principal = new User(login, "password", grantedAuthorities);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, grantedAuthorities);
        getContext().setAuthentication(authentication);
    }

    @ParameterizedTest
    @CsvSource({
            "true,false",
            "false,true"
    })
    void isAccessAllowed(boolean createDeniedFile, boolean expectedIsAccessAllowed) throws IOException {
        String tmpdir = getProperty("java.io.tmpdir");
        doReturn(tmpdir)
                .when(idsConfiguration)
                .getAbsoluteImagesDirectory();

        String rootDirectory = "ids-backend.root";
        temporaryBaseDirectory = new File(tmpdir + separator + rootDirectory);

        String idsHiddenDirectory = temporaryBaseDirectory + separator + ".ids";
        createDirectories(get(idsHiddenDirectory));

        String login = "login#1";
        if (createDeniedFile && !new File(idsHiddenDirectory + separator + "access.denied." + login).createNewFile()) {
            throw new RuntimeException("Fail to create access denied file");
        }

        doReturn(of(rootDirectory))
                .when(securityService)
                .getDirectoryPaths(rootDirectory);

        setAuthentication(login);

        assertEquals(expectedIsAccessAllowed, securityService.isAccessAllowed(rootDirectory));
    }

    @Test
    void isAccessAllowed() {
        String relativePath = "a/b/c";
        Directory directory = new Directory(null, new File(relativePath));

        boolean isAccessAllowed = true;
        doReturn(isAccessAllowed)
                .when(securityService)
                .isAccessAllowed(relativePath);

        assertEquals(isAccessAllowed, securityService.isAccessAllowed(directory));
    }

    @Test
    void checkAccessAllowed_allowed() {
        String fileSystemElementId = "aa/bb/cc";

        doReturn(true)
                .when(securityService)
                .isAccessAllowed(fileSystemElementId);

        assertDoesNotThrow(() -> securityService.checkAccessAllowed(fileSystemElementId));
    }

    @Test
    void checkAccessAllowed_denied() {
        String fileSystemElementId = "bb/cc/dd";

        doReturn(false)
                .when(securityService)
                .isAccessAllowed(fileSystemElementId);

        setAuthentication("login#2");

        assertThrows(AccessDeniedException.class, () -> securityService.checkAccessAllowed(fileSystemElementId));
    }
}
