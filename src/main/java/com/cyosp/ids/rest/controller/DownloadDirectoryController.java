package com.cyosp.ids.rest.controller;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
@RequestMapping(value = DownloadDirectoryController.DOWNLOAD_PATH, produces = "application/zip")
public class DownloadDirectoryController {
    static final String DOWNLOAD_PATH = "/download";

    private final IdsConfiguration idsConfiguration;
    private final ModelService modelService;
    private final SecurityService securityService;

    @GetMapping(path = "**")
    public void download(HttpServletRequest request, HttpServletResponse response) {
        String fullPathUrlEncoded = request.getRequestURL().toString().split(DOWNLOAD_PATH)[1];
        String fullPath = decode(fullPathUrlEncoded, UTF_8);
        log.info("Download asked: " + fullPath);

        String downloadExtension = ".zip";
        if (!fullPath.endsWith(downloadExtension)) {
            throw new IllegalStateException("Missing extension: " + downloadExtension);
        }
        String directoryPath = fullPath.substring(0, fullPath.lastIndexOf(downloadExtension));
        securityService.checkAccessAllowed(directoryPath);

        Path absoluteDirectoryPath = get(idsConfiguration.getAbsoluteImagesDirectory(), separator, directoryPath);
        if (!exists(absoluteDirectoryPath) || !isDirectory(absoluteDirectoryPath)) {
            throw new IllegalStateException("Path doesn't match an existing directory: " + fullPath);
        }

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());
             DirectoryStream<Path> paths = newDirectoryStream(absoluteDirectoryPath, modelService::isImage)) {
            response.setStatus(SC_OK);
            for (Path path : paths) {
                File file = path.toFile();
                zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
                FileInputStream fileInputStream = new FileInputStream(file);
                copy(fileInputStream, zipOutputStream);
                fileInputStream.close();
            }
            zipOutputStream.closeEntry();
        } catch (IOException e) {
            log.warn("Fail to list file system elements: " + e.getMessage());
        }
    }
}
