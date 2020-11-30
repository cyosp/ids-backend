package com.cyosp.ids.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static java.net.URI.create;
import static java.nio.file.Path.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ModelServiceTest {

    @InjectMocks
    private ModelService modelService;

    @Test
    void lowerCaseExtension() {
        Path path = of(create("file:///image1.jpg"));

        String lowerCaseExtension = modelService.lowerCaseExtension(path);

        assertEquals(".jpg", lowerCaseExtension);
    }

    @Test
    void lowerCaseExtension_UpperCaseExtension() {
        Path path = of(create("file:///image2.JPG"));

        String lowerCaseExtension = modelService.lowerCaseExtension(path);

        assertEquals(".jpg", lowerCaseExtension);
    }

    @Test
    void lowerCaseExtension_NoExtension() {
        Path path = of(create("file:///image3"));

        String lowerCaseExtension = modelService.lowerCaseExtension(path);

        assertEquals("", lowerCaseExtension);
    }
}