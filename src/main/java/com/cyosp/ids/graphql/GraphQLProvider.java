package com.cyosp.ids.graphql;

import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.google.common.io.Resources;
import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
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
public class GraphQLProvider {
    private static final String QUERY = "Query";
    private static final String MUTATION = "Mutation";

    private final GraphQLDataFetchers graphQLDataFetchers;

    private GraphQL graphQL;

    GraphQLProvider(GraphQLDataFetchers graphQLDataFetchers) {
        this.graphQLDataFetchers = graphQLDataFetchers;
    }

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
                .type(newTypeWiring(Directory.class.getSimpleName())
                        .dataFetcher("elements", graphQLDataFetchers.getDirectoryElementsDataFetcher()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("list", graphQLDataFetchers.getFileSystemElementsDataFetcher()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("getImages", graphQLDataFetchers.getImages()))
                .type(newTypeWiring(QUERY)
                        .dataFetcher("users", graphQLDataFetchers.getUsersDataFetcher()))
                .type(newTypeWiring(MUTATION)
                        .dataFetcher("generateAlternativeFormats", graphQLDataFetchers.generateAlternativeFormats()))
                .type(newTypeWiring(MUTATION)
                        .dataFetcher("changePassword", graphQLDataFetchers.changePassword()))
                .type(newTypeWiring(MUTATION)
                        .dataFetcher("deleteImage", graphQLDataFetchers.deleteImage()))
                .build();
    }

    @Bean
    public GraphQL graphQL() {
        return graphQL;
    }
}
