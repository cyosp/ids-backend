package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FileSystemElementServiceTest {
    @Mock
    private IdsConfiguration idsConfiguration;
    @Mock
    private ModelService modelService;
    @Mock
    private SecurityService securityService;

    private FileSystemElementService fileSystemElementService;

    @BeforeEach
    void beforeEach() {
        fileSystemElementService = spy(new FileSystemElementService(idsConfiguration, modelService, securityService));
    }

    @Test
    void list_accessDenied() {
        String directoryString = "a/b/c";
        Path directoryPath = Path.of(directoryString + "/d");
        Path imagePath = Path.of(directoryString + "/d/e.jpg");
        List<Path> unorderedPaths = asList(directoryPath, imagePath);
        doReturn(unorderedPaths)
                .when(fileSystemElementService)
                .getUnorderedPaths(directoryString);

        doReturn(true)
                .when(modelService)
                .isDirectory(directoryPath);
        doReturn(false)
                .when(modelService)
                .isDirectory(imagePath);
        Directory directory = new Directory(null, directoryPath.toFile());
        doReturn(directory)
                .when(modelService)
                .directoryFrom(directoryPath);

        doReturn(false)
                .when(modelService)
                .isMedia(directoryPath);
        doReturn(true)
                .when(modelService)
                .isMedia(imagePath);
        Image image = Image.from(null, imagePath.toFile());
        doReturn(image)
                .when(modelService)
                .mediaFrom(imagePath);

        List<FileSystemElement> fileSystemElements = fileSystemElementService.list(directoryString, false, false, false);

        assertTrue(fileSystemElements.isEmpty());
        verify(securityService, times(1)).isAccessAllowed(directory);
        verify(securityService, times(1)).isAccessAllowed(image);
    }
}
