package com.cyosp.ids.model;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.io.File;

import static java.io.File.separator;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.NONE;
import static lombok.AccessLevel.PRIVATE;

@Data
@Setter(value = NONE)
@Builder(access = PRIVATE)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Image extends FileSystemElement {
    public static final String IMAGES_URL_PATH_PREFIX = "/images/";
    public static final String FORMATS_URL_PATH_PREFIX = "/formats/";
    public static final String IDS_HIDDEN_DIRECTORY = ".ids";

    private String urlPath;

    private File previewFile;

    private String previewUrlPath;

    private File thumbnailFile;

    private String thumbnailUrlPath;

    private ImageMetadata metadata;

    public static Image from(String absoluteImagesDirectory, File relativeFile) {
        String name = relativeFile.getName();

        int dotIndex = name.lastIndexOf('.');
        String nameWithoutExtension = dotIndex > 0 ? name.substring(0, dotIndex) : name;

        String parentDirectory = relativeFile.getParent();
        parentDirectory = isNull(parentDirectory) ? "" : parentDirectory + separator;

        String previewPath = parentDirectory + IDS_HIDDEN_DIRECTORY + separator + nameWithoutExtension + ".preview.jpg";
        String thumbnailPath = parentDirectory + IDS_HIDDEN_DIRECTORY + separator + nameWithoutExtension + ".thumbnail.jpg";

        Image image = Image.builder()
                .urlPath(IMAGES_URL_PATH_PREFIX + parentDirectory + name)
                .previewFile(new File(absoluteImagesDirectory + separator + previewPath))
                .previewUrlPath(FORMATS_URL_PATH_PREFIX + previewPath)
                .thumbnailFile(new File(absoluteImagesDirectory + separator + thumbnailPath))
                .thumbnailUrlPath(FORMATS_URL_PATH_PREFIX + thumbnailPath)
                .build();
        image.setup(absoluteImagesDirectory, relativeFile);

        return image;
    }
}
