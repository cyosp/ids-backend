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
public class Image extends Media {
    public static Image from(String absoluteMediasDirectory, File relativeFile) {
        return (Image) Media.from(Image.class, absoluteMediasDirectory, relativeFile);
    }

    @Override
    protected String getPreviewExtension() {
        return ".jpg";
    }

    @Override
    protected String getThumbnailExtension() {
        return ".jpg";
    }
}
