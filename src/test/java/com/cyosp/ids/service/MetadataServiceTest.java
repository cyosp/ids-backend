package com.cyosp.ids.service;

import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.Metadata;
import com.cyosp.ids.model.Video;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static com.drew.imaging.ImageMetadataReader.readMetadata;
import static java.time.LocalDateTime.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class MetadataServiceTest {
    private final String imageMetadataAbsoluteFilePath = this.getClass().getResource("/media-metadata").getFile();

    @InjectMocks
    private MetadataService metadataService;

    @Test
    void getTakenDateFromExif() throws ImageProcessingException, IOException {
        com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(
                Image.from(imageMetadataAbsoluteFilePath, new File("TakenDateInExif.jpg")).getFile());

        LocalDateTime takenDate = metadataService.getTakenDateFromExif(metadata);

        assertEquals(parse("2008-08-21T19:52"), takenDate);
    }

    @Test
    void getTakenDateFromPictureInfo() {
        Image imageWithTakenDateInPictureInfo = Image.from(imageMetadataAbsoluteFilePath, new File("TakenDateInPictureInfo.jpg"));

        LocalDateTime takenDate = metadataService.getTakenDateFromPictureInfo(imageWithTakenDateInPictureInfo);

        assertEquals(parse("2001-04-16T12:15:28"), takenDate);
    }

    @Test
    void getTakenDateFromModifyDate() throws ImageProcessingException, IOException {
        com.drew.metadata.Metadata metadata = readMetadata(
                Image.from(imageMetadataAbsoluteFilePath, new File("TakenDateIsModifyDate.jpg")).getFile());

        LocalDateTime takenDate = metadataService.getTakenDateFromModifyDate(metadata);

        assertEquals(parse("2002-03-03T17:27:01"), takenDate);
    }

    @Test
    void getTakenDateFromQuickTimeMetadata() {
        Video video = Video.from(imageMetadataAbsoluteFilePath, new File("TakenDateFromQuickTimeMetadata.mov"));

        Metadata metadata = metadataService.from(video);

        assertEquals(parse("2025-08-26T08:09:12").toString(), metadata.getTakenAt());
    }

    @Test
    void getTakenDateFromCanonThumbnailDateTime() {
        Video video = Video.from(imageMetadataAbsoluteFilePath, new File("TakenDateFromCanonThumbnailDateTime.mov"));

        Metadata metadata = metadataService.from(video);

        assertEquals(parse("2025-08-26T08:19:54").toString(), metadata.getTakenAt());
    }
}
