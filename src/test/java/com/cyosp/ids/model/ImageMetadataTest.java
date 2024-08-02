package com.cyosp.ids.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.time.LocalDateTime;

import static java.time.LocalDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ImageMetadataTest {
    private final String IMAGE_METADATA_ABSOLUTE_FILE_PATH = this.getClass().getResource("/image-metadata").getFile();

    @Test
    void getTakenDateFromExif() {
        Image imageWithTakenDateInExif = Image.from(IMAGE_METADATA_ABSOLUTE_FILE_PATH, new File("TakenDateInExif.jpg"));

        LocalDateTime takenDate = ImageMetadata.getTakenDateFromExif(imageWithTakenDateInExif);

        assertEquals(parse("2008-08-21T19:52"), takenDate);
    }

    @Test
    void getTakenDateFromPictureInfo() {
        Image imageWithTakenDateInPictureInfo = Image.from(IMAGE_METADATA_ABSOLUTE_FILE_PATH, new File("TakenDateInPictureInfo.jpg"));

        LocalDateTime takenDate = ImageMetadata.getTakenDateFromPictureInfo(imageWithTakenDateInPictureInfo);

        assertEquals(parse("2001-04-16T12:15:28"), takenDate);
    }
}
