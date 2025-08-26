package com.cyosp.ids.rest.controller.admin;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.service.FileSystemElementService;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.io.File.separator;
import static java.net.URLDecoder.decode;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Paths.get;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.tomcat.util.http.fileupload.IOUtils.copy;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = RefreshStaticPreviewDirectoryController.DOWNLOAD_PATH, produces = "application/zip")
public class RefreshStaticPreviewDirectoryController {
    static final String DOWNLOAD_PATH = "/admin/refresh-static-preview-directory";

    private final SecurityService securityService;
    private final FileSystemElementService fileSystemElementService;

    @Async
    @GetMapping
    public void download() throws AccessDeniedException {
        securityService.checkAdministratorUser();
        fileSystemElementService.loadStaticPreviewDirectory();
    }
}
