package com.cyosp.ids.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.io.File;

import static lombok.AccessLevel.NONE;

@Data
@Setter(value = NONE)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class Video extends Media {
    public static Video from(String absoluteMediasDirectory, File relativeFile) {
        return (Video) Media.from(Video.class, absoluteMediasDirectory, relativeFile);
    }

    @Override
    protected String getPreviewExtension() {
        return ".mp4"; // Could be .webm
    }

    @Override
    protected String getThumbnailExtension() {
        return ".jpg";
    }
}
