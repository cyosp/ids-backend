package com.cyosp.ids.model;

import com.drew.metadata.exif.ExifSubIFDDirectory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

import static com.drew.imaging.ImageMetadataReader.readMetadata;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_DATETIME_ORIGINAL;
import static java.time.LocalDateTime.parse;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

@Data
@Slf4j
@Setter(value = NONE)
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class ImageMetadata {
    private String takenAt;

    public static ImageMetadata from(Image image) {
        try {
            ExifSubIFDDirectory exifSubIfdDirectory = readMetadata(image.getFile()).getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (nonNull(exifSubIfdDirectory)) {
                String originalDatetime = exifSubIfdDirectory.getString(TAG_DATETIME_ORIGINAL);
                try {
                    LocalDateTime originalLocalDatetime = parse(originalDatetime, ofPattern("yyyy:MM:dd HH:mm:ss"));
                    return new ImageMetadata(originalLocalDatetime.toString());
                } catch (Exception ex) {
                    log.warn("Original date time format not supported: {}", originalDatetime);
                }
            }
        } catch (Exception e) {
            log.warn("Fail to read image metadata: {}", e.getMessage());
        }
        return new ImageMetadata();
    }
}
