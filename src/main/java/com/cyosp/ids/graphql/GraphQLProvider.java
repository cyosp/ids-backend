package com.cyosp.ids.graphql;

import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.ImageMetadata;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.URL;

import static com.google.common.io.Resources.getResource;
import static graphql.GraphQL.newGraphQL;
import static graphql.schema.idl.RuntimeWiring.newRuntimeWiring;
import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;
import static java.nio.charset.StandardCharsets.UTF_8;

@Component
@RequiredArgsConstructor
public class GraphQLProvider {
    public  static final String IMAGE = "image";

    static final String DIRECTORY_REVERSED_ORDER = "directoryReversedOrder";
    static final String PREVIEW_DIRECTORY_REVERSED_ORDER = "previewDirectoryReversedOrder";
    static final String DIRECTORY = "directory";
    static final String FORCE_THUMBNAIL_GENERATION = "forceThumbnailGeneration";
    static final String PASSWORD = "password";
    static final String NEW_PASSWORD = "newPassword";

    private static final String QUERY = "Query";
    private static final String MUTATION = "Mutation";

    private final TypeDataFetcher typeDataFetcher;
    private final QueryDataFetcher queryDataFetcher;
    private final MutationDataFetcher mutationDataFetcher;

    private GraphQL graphQL;

    @PostConstruct
    public void init() throws IOException {
        URL url = getResource("schema.graphqls");
        String sdl = Resources.toString(url, UTF_8);
        GraphQLSchema graphQLSchema = buildSchema(sdl);
        graphQL = newGraphQL(graphQLSchema).build();
    }

    TypeResolver fileSystemElementTypeResolver = typeResolutionEnvironment -> {
        Object object = typeResolutionEnvironment.getObject();
        if (object instanceof Image) {
            return typeResolutionEnvironment.getSchema().getObjectType(Image.class.getSimpleName());
        } else if (object instanceof ImageMetadata) {
            return typeResolutionEnvironment.getSchema().getObjectType(ImageMetadata.class.getSimpleName());
        } else if (object instanceof Directory) {
            return typeResolutionEnvironment.getSchema().getObjectType(Directory.class.getSimpleName());
        } else {
            throw new UnsupportedOperationException();
        }
    };

    private GraphQLSchema buildSchema(String sdl) {
        TypeDefinitionRegistry typeRegistry = new SchemaParser().parse(sdl);
        RuntimeWiring runtimeWiring = buildWiring();
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        return schemaGenerator.makeExecutableSchema(typeRegistry, runtimeWiring);
    }

    private RuntimeWiring buildWiring() {
        return newRuntimeWiring()
                .type(FileSystemElement.class.getSimpleName(),
                        typeWriting -> typeWriting.typeResolver(fileSystemElementTypeResolver))
                .type(newTypeWiring(Image.class.getSimpleName())
                        .dataFetcher("metadata", typeDataFetcher.getImageMetadata()))
                .type(newTypeWiring(Directory.class.getSimpleName())
                        .dataFetcher("elements", typeDataFetcher.getDirectoryElementsDataFetcher()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("list", queryDataFetcher.getFileSystemElementsDataFetcher()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("getImages", queryDataFetcher.getImages()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("getImage", queryDataFetcher.getImage()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("users", queryDataFetcher.getUsersDataFetcher()))
                .type(newTypeWiring(MUTATION)
                        .dataFetcher("generateAlternativeFormats", mutationDataFetcher.generateAlternativeFormats()))
                .type(newTypeWiring(MUTATION)
                        .dataFetcher("changePassword", mutationDataFetcher.changePassword()))
                .type(newTypeWiring(MUTATION)
                        .dataFetcher("deleteImage", mutationDataFetcher.deleteImage()))
                .build();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }
}
