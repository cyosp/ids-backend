package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.Group;
import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
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
    private static final String A_ROLE = "ROLE";

    @Mock
    private IdsConfiguration idsConfiguration;
    @Mock
    private UserRepository userRepository;

    private SecurityService securityService;

    private AuthenticationTestService authenticationTestService;

    private File temporaryBaseDirectory;

    @BeforeEach
    void beforeEach() {
        securityService = spy(new SecurityService(idsConfiguration, userRepository, new ModelService(idsConfiguration)));
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
        authenticationTestService.setAuthenticatedUser("login#0", A_ROLE);

        assertTrue(securityService.hasAuthentication());
    }

    @Test
    void isAnonymousUser_no() {
        authenticationTestService.setAuthenticatedUser("login#1", A_ROLE);

        assertFalse(securityService.isAnonymousUser());
    }

    @Test
    void isAnonymousUser_yes() {
        authenticationTestService.setAnonymousUser();

        assertTrue(securityService.isAnonymousUser());
    }

    @Test
    void isGuestUser() {
        authenticationTestService.setAuthenticatedUser("123456", "GUEST");

        assertTrue(securityService.isGuestUser());
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
    void getPaths_null() {
        List<String> directories = securityService.getPaths(null);

        assertEquals(1, directories.size());
        assertEquals("", directories.get(0));
    }

    @Test
    void getPaths_empty() {
        List<String> directories = securityService.getPaths("");

        assertEquals(1, directories.size());
        assertEquals("", directories.get(0));
    }

    @Test
    void getPaths() {
        List<String> directories = securityService.getPaths("a/b/c");

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
        String login = "lo@in.in";
        User user = User.builder()
                .email(login)
                .build();
        doReturn(user)
                .when(userRepository)
                .getByEmail(login);

        String tmpdir = getProperty("java.io.tmpdir");
        doReturn(tmpdir)
                .when(idsConfiguration)
                .getAbsoluteMediasDirectory();

        String rootDirectory = "ids-backend.root";
        temporaryBaseDirectory = new File(tmpdir + separator + rootDirectory);

        String idsHiddenDirectory = temporaryBaseDirectory + separator + ".ids";
        createDirectories(get(idsHiddenDirectory));


        if (createDeniedFile && !new File(idsHiddenDirectory + separator + "access.denied." + login).createNewFile()) {
            throw new RuntimeException("Fail to create access denied file");
        }

        doReturn(of(rootDirectory))
                .when(securityService)
                .getPaths(rootDirectory);

        authenticationTestService.setAuthenticatedUser(login, A_ROLE);

        assertEquals(expectedIsAccessAllowed, securityService.isAccessAllowed(rootDirectory));
    }

    @ParameterizedTest
    @CsvSource({
            "ids-backend.root,true",
            "non-user-home-dir,false",
    })
    void isAccessAllowed_guestUser(String userHome, boolean expectedIsAccessAllowed) {
        doReturn(true)
                .when(securityService)
                .needAccessCheck();

        String guestLogin = "guestLogin";
        authenticationTestService.setAuthenticatedUser(guestLogin, "GUEST");

        String rootDirectory = "ids-backend.root";
        temporaryBaseDirectory = new File(getProperty("java.io.tmpdir") + separator + rootDirectory);

        User user = User.builder()
                .email(guestLogin)
                .home(userHome)
                .build();
        doReturn(user)
                .when(userRepository)
                .getByEmail(guestLogin);

        doReturn(true)
                .when(securityService)
                .isGuestUser();

        assertEquals(expectedIsAccessAllowed, securityService.isAccessAllowed(rootDirectory));
    }

    @ParameterizedTest
    @CsvSource({
            "false,a-login,,true",
            "true,a-login,,false",
            "true,a-login,a-group,false",
            "true,a-login,group-name,true",
            "true,login,a-group,true",
            "true,login,group-name,true",
    })
    void isAccessAllowed_limitedAccess(boolean createLimitedAccessFile, String userLogin, String userGroupName, boolean expectedIsAccessAllowed) throws IOException {
        doReturn(true)
                .when(securityService)
                .needAccessCheck();

        authenticationTestService.setAuthenticatedUser(userLogin, A_ROLE);

        String tmpdir = getProperty("java.io.tmpdir");
        String rootDirectory = "ids-backend.root";
        temporaryBaseDirectory = new File(tmpdir + separator + rootDirectory);

        doReturn(of(rootDirectory))
                .when(securityService)
                .getPaths(rootDirectory);

        User user = User.builder()
                .email(userLogin)
                .groups(of(Group.builder()
                        .name(userGroupName)
                        .build()))
                .build();
        doReturn(user)
                .when(userRepository)
                .getByEmail(userLogin);

        doReturn(false)
                .when(securityService)
                .isGuestUser();

        doReturn(tmpdir)
                .when(idsConfiguration)
                .getAbsoluteMediasDirectory();

        String idsHiddenDirectory = temporaryBaseDirectory + separator + ".ids";
        createDirectories(get(idsHiddenDirectory));

        if (createLimitedAccessFile && !new File(idsHiddenDirectory + separator + "access.limited.user.name.login").createNewFile()) {
            throw new RuntimeException("Fail to create access limited file");
        }
        if (createLimitedAccessFile && !new File(idsHiddenDirectory + separator + "access.limited.group.name.group-name").createNewFile()) {
            throw new RuntimeException("Fail to create access limited file");
        }

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

        authenticationTestService.setAuthenticatedUser("login#3", A_ROLE);

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
