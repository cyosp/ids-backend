package com.cyosp.ids.graphql;

import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
import com.cyosp.ids.service.FileSystemElementService;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.SecurityService;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.cyosp.ids.graphql.GraphQLProvider.DIRECTORY;
import static com.cyosp.ids.graphql.GraphQLProvider.DIRECTORY_REVERSED_ORDER;
import static com.cyosp.ids.graphql.GraphQLProvider.IMAGE;
import static com.cyosp.ids.graphql.GraphQLProvider.PREVIEW_DIRECTORY_REVERSED_ORDER;
import static java.lang.Boolean.TRUE;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;

@Component
@RequiredArgsConstructor
public class QueryDataFetcher {
    private final UserRepository userRepository;
    private final ModelService modelService;
    private final FileSystemElementService fileSystemElementService;
    private final SecurityService securityService;

    public DataFetcher<List<FileSystemElement>> getFileSystemElementsDataFetcher() {
        return dataFetchingEnvironment -> {
            String directory = dataFetchingEnvironment.getArgument(DIRECTORY);
            securityService.checkAccessAllowed(directory);

            GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();

            boolean directoryReversedOrder = TRUE.equals(dataFetchingEnvironment.getArgument(DIRECTORY_REVERSED_ORDER));
            graphQLContext.put(DIRECTORY_REVERSED_ORDER, directoryReversedOrder);

            boolean previewDirectoryReversedOrder = TRUE.equals(dataFetchingEnvironment.getArgument(PREVIEW_DIRECTORY_REVERSED_ORDER));
            graphQLContext.put(PREVIEW_DIRECTORY_REVERSED_ORDER, previewDirectoryReversedOrder);

            return new ArrayList<>(fileSystemElementService.listFileSystemElements(directory, directoryReversedOrder, previewDirectoryReversedOrder));
        };
    }

    public DataFetcher<Image> getImage() {
        return dataFetchingEnvironment -> {
            Image image = modelService.getImage(dataFetchingEnvironment);
            securityService.checkAccessAllowed(image);
            return image;
        };
    }

    public DataFetcher<List<Image>> getImages() {
        return dataFetchingEnvironment -> {
            String directory = dataFetchingEnvironment.getArgument(DIRECTORY);
            securityService.checkAccessAllowed(directory);

            Iterator<Path> pathIterator = Files.list(get(fileSystemElementService.getAbsoluteDirectoryPath(directory)))
                    .filter(modelService::isImage)
                    .sorted(comparing(path -> path.getFileName().toString()))
                    .iterator();

            String image = dataFetchingEnvironment.getArgument(IMAGE);
            Path currentPath = null;
            while (pathIterator.hasNext()) {
                Path previousPath = currentPath;
                currentPath = pathIterator.next();
                if (currentPath.getFileName().toString().equals(image)) {
                    return asList(nonNull(previousPath) ? modelService.imageFrom(previousPath) : null,
                            modelService.imageFrom(currentPath),
                            pathIterator.hasNext() ? modelService.imageFrom(pathIterator.next()) : null);
                }
            }

            return asList(null, null, null);
        };
    }

    public DataFetcher<List<User>> getUsersDataFetcher() {
        return dataFetchingEnvironment -> {
            securityService.checkAdministratorUser();
            return userRepository.findAll();
        };
    }
}
