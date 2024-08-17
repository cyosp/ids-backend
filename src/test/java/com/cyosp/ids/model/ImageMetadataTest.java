package com.cyosp.ids.model;

import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static com.drew.imaging.ImageMetadataReader.readMetadata;
import static java.time.LocalDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ImageMetadataTest {
    private final String imageMetadataAbsoluteFilePath = this.getClass().getResource("/image-metadata").getFile();

    @Test
    void getTakenDateFromExif() throws ImageProcessingException, IOException {
        Metadata metadata = readMetadata(
                Image.from(imageMetadataAbsoluteFilePath, new File("TakenDateInExif.jpg")).getFile());

        LocalDateTime takenDate = ImageMetadata.getTakenDateFromExif(metadata);

        assertEquals(parse("2008-08-21T19:52"), takenDate);
    }

    @Test
    void getTakenDateFromPictureInfo() {
        Image imageWithTakenDateInPictureInfo = Image.from(imageMetadataAbsoluteFilePath, new File("TakenDateInPictureInfo.jpg"));

        LocalDateTime takenDate = ImageMetadata.getTakenDateFromPictureInfo(imageWithTakenDateInPictureInfo);

        assertEquals(parse("2001-04-16T12:15:28"), takenDate);
    }

    @Test
    void getTakenDateFromModifyDate() throws ImageProcessingException, IOException {
        Metadata metadata = readMetadata(
                Image.from(imageMetadataAbsoluteFilePath, new File("TakenDateIsModifyDate.jpg")).getFile());

        LocalDateTime takenDate = ImageMetadata.getTakenDateFromModifyDate(metadata);

        assertEquals(parse("2002-03-03T17:27:01"), takenDate);
    }
}
