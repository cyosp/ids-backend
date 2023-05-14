package com.cyosp.ids.graphql;

import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.ImageMetadata;
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
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class TypeDataFetcherTest {
    @Mock
    private SecurityService securityService;

    @InjectMocks
    private TypeDataFetcher typeDataFetcher;

    @Mock
    private DataFetchingEnvironment dataFetchingEnvironment;

    @Test
    void getImageMetadata_accessDenied() {
        Image image = Image.from("/absolute-images-directory", new File("a/b/c/d.jpg"));
        doReturn(image)
                .when(dataFetchingEnvironment)
                .getSource();

        doThrow(AccessDeniedException.class)
                .when(securityService)
                .checkAccessAllowed(image);

        DataFetcher<ImageMetadata> imageMetadataDataFetcher = typeDataFetcher.getImageMetadata();

        assertThrows(AccessDeniedException.class, () -> imageMetadataDataFetcher.get(dataFetchingEnvironment));
    }

    @Test
    void getDirectoryElementsDataFetcher_accessDenied() throws Exception {
        Directory directory = new Directory("/absolute-images-directory", new File("a/b/c"));
        doReturn(directory)
                .when(dataFetchingEnvironment)
                .getSource();

        doReturn(false)
                .when(securityService)
                .isAccessAllowed(directory);

        DataFetcher<List<FileSystemElement>> fileSystemElementsDataFetcher = typeDataFetcher.getDirectoryElementsDataFetcher();

        assertEquals(new ArrayList<>(), fileSystemElementsDataFetcher.get(dataFetchingEnvironment));
    }
}
