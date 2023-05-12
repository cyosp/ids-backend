package com.cyosp.ids.graphql;

import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.repository.UserRepository;
import com.cyosp.ids.service.FileSystemElementService;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.SecurityService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class QueryDataFetcherTest {
    private final Class<AccessDeniedException> accessDeniedExceptionClass = AccessDeniedException.class;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ModelService modelService;
    @Mock
    private FileSystemElementService fileSystemElementService;
    @Mock
    private SecurityService securityService;

    @InjectMocks
    private QueryDataFetcher queryDataFetcher;

    @Mock
    private DataFetchingEnvironment dataFetchingEnvironment;

    @Test
    void getFileSystemElementsDataFetcher_accessDenied() {
        String directory = "a/b/c";
        doReturn(directory)
                .when(dataFetchingEnvironment)
                .getArgument("directory");

        doThrow(accessDeniedExceptionClass)
                .when(securityService)
                .checkAccessAllowed(directory);

        DataFetcher<List<FileSystemElement>> fileSystemElementsDataFetcher = queryDataFetcher.getFileSystemElementsDataFetcher();

        assertThrows(accessDeniedExceptionClass, () -> fileSystemElementsDataFetcher.get(dataFetchingEnvironment));
    }

    @Test
    void getImage_accessDenied() {
        Image image = Image.from(null, new File("a/b/c/d.jpg"));
        doReturn(image)
                .when(modelService)
                .getImage(dataFetchingEnvironment);

        doThrow(accessDeniedExceptionClass)
                .when(securityService)
                .checkAccessAllowed(image);

        DataFetcher<Image> imageDataFetcher = queryDataFetcher.getImage();

        assertThrows(accessDeniedExceptionClass, () -> imageDataFetcher.get(dataFetchingEnvironment));
    }

    @Test
    void getImages_accessDenied() {
        String directory = "aa/bb/cc";
        doReturn(directory)
                .when(dataFetchingEnvironment)
                .getArgument("directory");

        doThrow(accessDeniedExceptionClass)
                .when(securityService)
                .checkAccessAllowed(directory);

        DataFetcher<List<Image>> imagesDataFetcher = queryDataFetcher.getImages();

        assertThrows(accessDeniedExceptionClass, () -> imagesDataFetcher.get(dataFetchingEnvironment));
    }
}
