package com.cyosp.ids.model;

import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.google.common.annotations.VisibleForTesting;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;

import static com.drew.imaging.ImageMetadataReader.readMetadata;
import static com.drew.metadata.exif.ExifDirectoryBase.TAG_DATETIME_ORIGINAL;
import static com.twelvemonkeys.imageio.metadata.jpeg.JPEG.APP12;
import static com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegmentUtil.readSegments;
import static java.lang.Long.parseLong;
import static java.time.Instant.ofEpochMilli;
import static java.time.LocalDateTime.ofInstant;
import static java.time.LocalDateTime.parse;
import static java.time.ZoneId.of;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Objects.nonNull;
import static java.util.regex.Pattern.compile;
import static javax.imageio.ImageIO.createImageInputStream;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

@Data
@Slf4j
@Setter(value = NONE)
@NoArgsConstructor(access = PRIVATE)
@AllArgsConstructor(access = PRIVATE)
public class ImageMetadata {
    private static final int TAG_MODIFY_DATE = 306;

    private String takenAt;

    private static LocalDateTime getLocalDateTime(String originalDatetime) {
        try {
            return parse(originalDatetime, ofPattern("yyyy:MM:dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("Original date time format not supported: {}", originalDatetime);
            return null;
        }
    }

    @VisibleForTesting
    static LocalDateTime getTakenDateFromExif(Metadata metadata) {
        ExifDirectoryBase exifDirectoryBase = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (nonNull(exifDirectoryBase)) {
            return getLocalDateTime(exifDirectoryBase.getString(TAG_DATETIME_ORIGINAL));
        }
        return null;
    }

    @VisibleForTesting
    static LocalDateTime getTakenDateFromPictureInfo(Image image) {
        List<JPEGSegment> jpegSegments;
        try {
            jpegSegments = readSegments(createImageInputStream(image.getFile()), APP12, null);
        } catch (IOException e) {
            if (!"Not a JPEG stream".equals(e.getMessage())) {
                log.error("Fail to read JPEG segments: {}", e.getMessage());
            }
            return null;
        }

        for (JPEGSegment jpegSegment : jpegSegments) {
            // https://www.ozhiker.com/electronics/pjmt/jpeg_info/app_segments.html
            if (jpegSegment.identifier().trim().equals("OLYMPUS OPTICAL CO.,LTD.")) {
                try (InputStream jpegSegmentDataStream = jpegSegment.data()) {
                    String jpegSegmentData = new String(jpegSegmentDataStream.readAllBytes());
                    // https://www.exiftool.org/TagNames/APP12.html
                    Matcher matcher = compile("TimeDate=(\\d+)").matcher(jpegSegmentData);
                    if (matcher.find()) {
                        return ofInstant(ofEpochMilli(parseLong(matcher.group(1)) * 1000), of("UTC"));
                    }
                } catch (IOException e) {
                    log.error("Fail to read data JPEG segment: {}", e.getMessage());
                }
            }
        }
        return null;
    }

    @VisibleForTesting
    static LocalDateTime getTakenDateFromModifyDate(Metadata metadata) {
        ExifDirectoryBase exifDirectoryBase = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (nonNull(exifDirectoryBase)) {
            return getLocalDateTime(exifDirectoryBase.getString(TAG_MODIFY_DATE));
        }
        return null;
    }

    private static LocalDateTime getTakenDate(Image image) {
        try {
            Metadata metadata = readMetadata(image.getFile());

            LocalDateTime takenDateFromExif = getTakenDateFromExif(metadata);
            if (nonNull(takenDateFromExif)) {
                return takenDateFromExif;
            }

            LocalDateTime takenDateFromPictureInfo = getTakenDateFromPictureInfo(image);
            if (nonNull(takenDateFromPictureInfo)) {
                return takenDateFromPictureInfo;
            }

            LocalDateTime takenDateFromModifyDate = getTakenDateFromModifyDate(metadata);
            if (nonNull(takenDateFromModifyDate)) {
                return takenDateFromModifyDate;
            }
        } catch (Exception e) {
            log.error("Fail to read image metadata: {}", e.getMessage());
        }
        return null;
    }

    public static ImageMetadata from(Image image) {
        LocalDateTime takenDate = getTakenDate(image);
        return new ImageMetadata(nonNull(takenDate) ? takenDate.toString() : null);
    }
}
