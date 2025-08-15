package com.cyosp.ids.service;

import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.Metadata;
import com.cyosp.ids.model.Video;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.mov.metadata.QuickTimeMetadataDirectory;
import com.google.common.annotations.VisibleForTesting;
import com.twelvemonkeys.imageio.metadata.jpeg.JPEGSegment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.stream.StreamSupport;

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
import static java.util.stream.Collectors.toList;
import static javax.imageio.ImageIO.createImageInputStream;

@Slf4j
@Service
public class MetadataService {
    private static final int TAG_MODIFY_DATE = 306;

    private LocalDateTime getLocalDateTimeFromLocal(String originalDatetime) {
        try {
            return parse(originalDatetime, ofPattern("yyyy:MM:dd HH:mm:ss"));
        } catch (Exception e) {
            log.error("Original date time format not supported: {}", originalDatetime);
            return null;
        }
    }

    @VisibleForTesting
    LocalDateTime getTakenDateFromExif(com.drew.metadata.Metadata metadata) {
        ExifDirectoryBase exifDirectoryBase = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
        if (nonNull(exifDirectoryBase)) {
            return getLocalDateTimeFromLocal(exifDirectoryBase.getString(TAG_DATETIME_ORIGINAL));
        }
        return null;
    }

    @VisibleForTesting
    LocalDateTime getTakenDateFromPictureInfo(Image image) {
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
    LocalDateTime getTakenDateFromModifyDate(com.drew.metadata.Metadata metadata) {
        ExifDirectoryBase exifDirectoryBase = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (nonNull(exifDirectoryBase)) {
            return getLocalDateTimeFromLocal(exifDirectoryBase.getString(TAG_MODIFY_DATE));
        }
        return null;
    }

    private LocalDateTime getTakenDate(Image image) {
        try {
            com.drew.metadata.Metadata metadata = readMetadata(image.getFile());

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

    public Metadata from(Image image) {
        LocalDateTime takenDate = getTakenDate(image);
        return new Metadata(nonNull(takenDate) ? takenDate.toString() : null);
    }

    private List<String> getDates(com.drew.metadata.Metadata metadata) {
        return metadata.getDirectoriesOfType(QuickTimeMetadataDirectory.class).stream()
                .filter(Objects::nonNull)
                .map(QuickTimeMetadataDirectory::getTags)
                .flatMap(Collection::stream)
                .filter(tag -> tag.getTagName().equals("Creation Date"))
                .findFirst()
                .map(Tag::getDescription)
                .map(List::of)
                .orElseGet(() -> StreamSupport.stream(metadata.getDirectories().spliterator(), false)
                        .flatMap(directory -> directory.getTags().stream())
                        .filter(tag -> tag.getTagName().contains("Date"))
                        .map(Tag::getDescription)
                        .collect(toList()));
    }

    private LocalDateTime parseDate(String date) {
        try {
            return parse(date, ofPattern("yyyy-MM-dd'T'HH:mm:ssZ"));
        } catch (DateTimeParseException e1) {
            try {
                return parse(date, ofPattern("yyyy:MM:dd HH:mm:ss"));
            } catch (DateTimeParseException e2) {
                log.error("Date format not supported: {}", date);
                return null;
            }
        }
    }

    public Metadata from(Video video) {
        try {
            return getDates(readMetadata(video.getFile())).stream()
                    .map(this::parseDate)
                    .filter(Objects::nonNull)
                    .sorted()
                    .findFirst()
                    .map(LocalDateTime::toString)
                    .map(Metadata::new)
                    .orElse(null);
        } catch (Exception e) {
            log.error("Fail to read video metadata: {}", e.getMessage());
            return null;
        }
    }
}
