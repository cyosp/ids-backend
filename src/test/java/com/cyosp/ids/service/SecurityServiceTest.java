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

import java.io.File;
import java.io.IOException;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;
import static org.springframework.util.FileSystemUtils.deleteRecursively;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {
    @Mock
    private IdsConfiguration idsConfiguration;

    private SecurityService securityService;

    private AuthenticationTestService authenticationTestService;

    private File temporaryBaseDirectory;

    @BeforeEach
    void beforeEach() {
        securityService = spy(new SecurityService(idsConfiguration));
        authenticationTestService = new AuthenticationTestService();
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
    void hasAuthentication_no() {
        getContext().setAuthentication(null);

        assertFalse(securityService.hasAuthentication());
    }

    @Test
    void hasAuthentication_yes() {
        authenticationTestService.setAuthenticatedUser("login#0");

        assertTrue(securityService.hasAuthentication());
    }

    @Test
    void isAnonymousUser_no() {
        authenticationTestService.setAuthenticatedUser("login#1");

        assertFalse(securityService.isAnonymousUser());
    }

    @Test
    void isAnonymousUser_yes() {
        authenticationTestService.setAnonymousUser();

        assertTrue(securityService.isAnonymousUser());
    }

    @ParameterizedTest
    @CsvSource({
            "false,,false",
            "true,false,true",
            "true,true,false"
    })
    void needAccessCheck(boolean hasAuthentication, Boolean isAnonymousUser, boolean expectedNeedAccessCheck) {
        doReturn(hasAuthentication)
                .when(securityService)
                .hasAuthentication();

        if (nonNull(isAnonymousUser)) {
            doReturn(isAnonymousUser)
                    .when(securityService)
                    .isAnonymousUser();
        }

        assertEquals(expectedNeedAccessCheck, securityService.needAccessCheck());
    }

    @Test
    void getParent() {
        assertEquals("/a/b", securityService.getParent("/a/b/c"));
    }

    @Test
    void getDirectoryPaths_null() {
        List<String> directories = securityService.getDirectoryPaths(null);

        assertEquals(1, directories.size());
        assertEquals("", directories.get(0));
    }

    @Test
    void getDirectoryPaths_empty() {
        List<String> directories = securityService.getDirectoryPaths("");

        assertEquals(1, directories.size());
        assertEquals("", directories.get(0));
    }

    @Test
    void getDirectoryPaths() {
        List<String> directories = securityService.getDirectoryPaths("a/b/c");

        assertEquals(asList("a", "a/b", "a/b/c"), directories);
    }

    @Test
    void isAccessAllowed_dontNeedAccessCheck() {
        doReturn(false)
                .when(securityService)
                .needAccessCheck();

        assertTrue(securityService.isAccessAllowed("a/b"));
    }

    @ParameterizedTest
    @CsvSource({
            "true,false",
            "false,true"
    })
    void isAccessAllowed_needAccessCheck(boolean createDeniedFile, boolean expectedIsAccessAllowed) throws IOException {
        doReturn(true)
                .when(securityService)
                .needAccessCheck();

        String tmpdir = getProperty("java.io.tmpdir");
        doReturn(tmpdir)
                .when(idsConfiguration)
                .getAbsoluteMediasDirectory();

        String rootDirectory = "ids-backend.root";
        temporaryBaseDirectory = new File(tmpdir + separator + rootDirectory);

        String idsHiddenDirectory = temporaryBaseDirectory + separator + ".ids";
        createDirectories(get(idsHiddenDirectory));

        String login = "login#2";
        if (createDeniedFile && !new File(idsHiddenDirectory + separator + "access.denied." + login).createNewFile()) {
            throw new RuntimeException("Fail to create access denied file");
        }

        doReturn(of(rootDirectory))
                .when(securityService)
                .getDirectoryPaths(rootDirectory);

        authenticationTestService.setAuthenticatedUser(login);

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

        authenticationTestService.setAuthenticatedUser("login#3");

        assertThrows(AccessDeniedException.class, () -> securityService.checkAccessAllowed(fileSystemElementId));
    }

    @Test
    void checkAccessAllowed() {
        String relativePath = "aaa/bbb/ccc";
        Directory directory = new Directory(null, new File(relativePath));

        Class<AccessDeniedException> accessDeniedExceptionClass = AccessDeniedException.class;
        doThrow(accessDeniedExceptionClass)
                .when(securityService)
                .checkAccessAllowed(relativePath);

        assertThrows(accessDeniedExceptionClass, () -> securityService.checkAccessAllowed(directory));
    }
}
