package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.google.common.annotations.VisibleForTesting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.io.File.separator;
import static java.lang.String.format;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Paths.get;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemElementService {
    private final StopWatch stopWatch = new StopWatch();
    private final IdsConfiguration idsConfiguration;
    private final ModelService modelService;
    private final SecurityService securityService;

    private final Map<String, Image> previewDirectoryNaturalOrderMap = new HashMap<>();
    private final Map<String, Image> previewDirectoryReversedOrderMap = new HashMap<>();
    private boolean previewDirectoryLoaded = false;

    @EventListener(ApplicationReadyEvent.class)
    public void loadStaticPreviewDirectory() {
        if (idsConfiguration.isStaticPreviewDirectory()) {
            log.info("Static preview directory loading");
            stopWatch.start();
            list(null, true, false, false);
            stopWatch.stop();
            previewDirectoryLoaded = true;
            log.info(format("Static preview directory loaded in %s ms", stopWatch.getTotalTimeMillis()));
        }
    }

    public String getAbsoluteDirectoryPath(String relativeDirectory) {
        final StringBuilder absoluteDirectoryPath = new StringBuilder(idsConfiguration.getAbsoluteImagesDirectory());
        if (ofNullable(relativeDirectory).isPresent())
            absoluteDirectoryPath.append(separator).append(relativeDirectory);
        return absoluteDirectoryPath.toString();
    }

    public List<Image> listImagesInAllDirectories(String directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        List<Image> images = new ArrayList<>();
        for (FileSystemElement fileSystemElement : listFileSystemElements(directory, directoryReversedOrder, previewDirectoryReversedOrder)) {
            if (fileSystemElement instanceof Image)
                images.add((Image) fileSystemElement);
            else {
                images.addAll(
                        listRecursively(fileSystemElement.getId(), directoryReversedOrder, previewDirectoryReversedOrder).stream()
                                .filter(Image.class::isInstance)
                                .map(Image.class::cast)
                                .collect(toList()));
            }
        }
        return images;
    }

    private List<FileSystemElement> listRecursively(String directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        return list(directory, true, directoryReversedOrder, previewDirectoryReversedOrder);
    }

    public List<FileSystemElement> listFileSystemElements(String directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        return list(directory, false, directoryReversedOrder, previewDirectoryReversedOrder);
    }

    public List<FileSystemElement> listFileSystemElements(Directory directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        return list(modelService.stringRelative(directory), false, directoryReversedOrder, previewDirectoryReversedOrder);
    }

    private Image preview(Directory directory, boolean previewDirectoryReversedOrder) {
        List<FileSystemElement> fileSystemElements = listFileSystemElements(directory, previewDirectoryReversedOrder, previewDirectoryReversedOrder);

        Image image = fileSystemElements.stream()
                .map(fse -> Path.of(fse.getFile().toURI()))
                .filter(modelService::isImage)
                .sorted(previewDirectoryReversedOrder ? reverseOrder() : naturalOrder())
                .map(modelService::imageFrom)
                .findFirst()
                .orElse(null);

        return ofNullable(image)
                .orElse(fileSystemElements.stream()
                        .map(fse -> Path.of(fse.getFile().toURI()))
                        .filter(modelService::isDirectory)
                        .map(modelService::directoryFrom)
                        .map(dir -> preview(dir, previewDirectoryReversedOrder))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    @VisibleForTesting
    List<Path> getUnorderedPaths(String directoryString) {
        List<Path> unorderedPaths = new ArrayList<>();
        try (DirectoryStream<Path> paths = newDirectoryStream(get(getAbsoluteDirectoryPath(directoryString)),
                path -> modelService.isImage(path) || modelService.isDirectory(path))) {
            paths.forEach(unorderedPaths::add);
        } catch (IOException e) {
            log.warn("Fail to list file system elements: " + e.getMessage());
        }
        return unorderedPaths;
    }

    private Comparator<FileSystemElement> byName() {
        return comparing(FileSystemElement::getName);
    }

    @VisibleForTesting
    List<FileSystemElement> list(String directoryString, boolean recursive, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        final List<FileSystemElement> fileSystemElements = new ArrayList<>();

        List<Path> unorderedPaths = getUnorderedPaths(directoryString);

        unorderedPaths.stream()
                .filter(modelService::isDirectory)
                .map(modelService::directoryFrom)
                .filter(securityService::isAccessAllowed)
                .sorted(directoryReversedOrder ? byName().reversed() : byName())
                .forEach(directory -> {
                    Image preview = null;
                    if (idsConfiguration.isStaticPreviewDirectory()) {
                        String directoryId = directory.getId();
                        if (previewDirectoryLoaded) {
                            preview = (previewDirectoryReversedOrder ? previewDirectoryReversedOrderMap
                                    : previewDirectoryNaturalOrderMap).get(directoryId);
                        } else {
                            // TODO Avoid to iterate twice
                            previewDirectoryNaturalOrderMap.put(directoryId,  preview(directory, false));
                            previewDirectoryReversedOrderMap.put(directoryId, preview(directory, true));
                        }
                    } else {
                        preview = preview(directory, previewDirectoryReversedOrder);
                    }
                    directory.setPreview(preview);
                    fileSystemElements.add(directory);
                    if (recursive) {
                        fileSystemElements.addAll(listRecursively(modelService.stringRelative(directory), directoryReversedOrder, previewDirectoryReversedOrder));
                    }
                });

        unorderedPaths.stream()
                .filter(modelService::isImage)
                .map(modelService::imageFrom)
                .filter(securityService::isAccessAllowed)
                .sorted(byName())
                .forEach(fileSystemElements::add);

        return fileSystemElements;
    }
}
