package com.cyosp.ids.graphql;

import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.ImageMetadata;
import com.cyosp.ids.service.FileSystemElementService;
import com.cyosp.ids.service.SecurityService;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.cyosp.ids.graphql.GraphQLProvider.DIRECTORY_REVERSED_ORDER;
import static com.cyosp.ids.graphql.GraphQLProvider.PREVIEW_DIRECTORY_REVERSED_ORDER;
import static java.lang.Boolean.TRUE;

@Component
@RequiredArgsConstructor
public class TypeDataFetcher {
    private final SecurityService securityService;
    private final FileSystemElementService fileSystemElementService;

    public DataFetcher<ImageMetadata> getImageMetadata() {
        return dataFetchingEnvironment -> {
            Image image = dataFetchingEnvironment.getSource();
            securityService.checkAccessAllowed(image);
            return ImageMetadata.from(image);
        };
    }

    public DataFetcher<List<FileSystemElement>> getDirectoryElementsDataFetcher() {
        return dataFetchingEnvironment -> {
            Directory directory = dataFetchingEnvironment.getSource();
            if (securityService.isAccessAllowed(directory)) {
                GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();

                boolean directoryReversedOrder = TRUE.equals(graphQLContext.get(DIRECTORY_REVERSED_ORDER));
                boolean previewDirectoryReversedOrder = TRUE.equals(graphQLContext.get(PREVIEW_DIRECTORY_REVERSED_ORDER));

                return new ArrayList<>(fileSystemElementService.listFileSystemElements(directory.getId(), directoryReversedOrder, previewDirectoryReversedOrder));
            } else {
                return new ArrayList<>();
            }
        };
    }
}
