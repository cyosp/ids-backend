package com.cyosp.ids.security.jwt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;

import static com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING;
import static java.time.OffsetDateTime.now;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest httpServletRequest,
                         HttpServletResponse httpServletResponse,
                         AuthenticationException authenticationException) throws IOException {
        httpServletResponse.setStatus(SC_UNAUTHORIZED);
        httpServletResponse.setContentType(APPLICATION_JSON_VALUE);
        httpServletResponse.getOutputStream().println(objectMapper.writeValueAsString(
                Payload.builder()
                        .timestamp(now())
                        .status(SC_UNAUTHORIZED)
                        .error("Unauthorized")
                        .message(authenticationException.getMessage())
                        .path(httpServletRequest.getRequestURI())
                        .build()
        ));
    }

    @Data
    @Builder
    private static class Payload {
        @JsonFormat(shape = STRING, pattern = "YYYY-MM-DD'T'HH:mm:ss.SSSZ")
        OffsetDateTime timestamp;

        int status;

        String error;

        String message;

        String path;
    }
}
