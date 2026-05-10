package com.cyosp.ids.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.PublicApi;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import graphql.execution.ExecutionPath;
import graphql.language.SourceLocation;
//import graphql.util.LogKit;
//import org.slf4j.Logger;

@PublicApi
public class CustomDataFetcherExceptionHandler implements DataFetcherExceptionHandler {

    //private static final Logger logNotSafe = LogKit.getNotPrivacySafeLogger(SimpleDataFetcherExceptionHandler.class);

    @Override
    public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters handlerParameters) {
        Throwable exception = handlerParameters.getException();
        SourceLocation sourceLocation = handlerParameters.getSourceLocation();
        ExecutionPath path = handlerParameters.getPath();

        ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(path, exception, sourceLocation);
        //logNotSafe.warn(error.getMessage(), exception);

        return DataFetcherExceptionHandlerResult.newResult().error(error).build();
    }
}
