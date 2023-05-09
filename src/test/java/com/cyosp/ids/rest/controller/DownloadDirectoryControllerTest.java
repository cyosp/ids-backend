package com.cyosp.ids.rest.controller;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.SecurityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class DownloadDirectoryControllerTest {
    @Mock
    private IdsConfiguration idsConfiguration;
    @Mock
    private ModelService modelService;
    @Mock
    private SecurityService securityService;

    @InjectMocks
    private DownloadDirectoryController downloadDirectoryController;

    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;

    @Test
    void download_accesDenied() {
        doReturn(new StringBuffer("/download/00%20-%20000/01%20-%20001/02%20-%20002/03%20-%20003/04%20-%20004.zip"))
                .when(request)
                .getRequestURL();

        doThrow(AccessDeniedException.class)
                .when(securityService)
                .checkAccessAllowed("/00 - 000/01 - 001/02 - 002/03 - 003/04 - 004");

        assertThrows(AccessDeniedException.class, () -> downloadDirectoryController.download(request, response));
    }
}
