package com.cyosp.ids.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.ToString;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static com.cyosp.ids.model.Directory.IDS_HIDDEN_DIRECTORY;
import static java.io.File.separator;
import static java.util.Objects.isNull;
import static lombok.AccessLevel.PACKAGE;

@Data
@Setter(value = PACKAGE)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class Media extends FileSystemElement {
    protected String urlPath;
    protected File previewFile;
    protected String previewUrlPath;
    protected File thumbnailFile;
    protected String thumbnailUrlPath;
    private Metadata metadata;

    public static String getFormatsUrlPathPrefix() {
        return "/formats/";
    }

    public static String getUrlPathPrefix(Class<?> clazz) {
        return "/" + clazz.getSimpleName().toLowerCase() + "s/";
    }

    protected String getUrlPathPrefix() {
        return getUrlPathPrefix(this.getClass());
    }

    protected abstract String getPreviewExtension();

    protected abstract String getThumbnailExtension();

    public static Media from(Class<? extends Media> mediaClass, String absoluteMediasDirectory, File relativeFile) {
        String name = relativeFile.getName();

        int dotIndex = name.lastIndexOf('.');
        String nameWithoutExtension = dotIndex > 0 ? name.substring(0, dotIndex) : name;

        String parentDirectory = relativeFile.getParent();
        parentDirectory = isNull(parentDirectory) ? "" : parentDirectory + separator;

        try {
            Constructor<? extends Media> mediaClassConstructor = mediaClass.getConstructor();
            Media media = mediaClassConstructor.newInstance();

            String previewPath = parentDirectory + IDS_HIDDEN_DIRECTORY + separator + nameWithoutExtension + ".preview" + media.getPreviewExtension();
            String thumbnailPath = parentDirectory + IDS_HIDDEN_DIRECTORY + separator + nameWithoutExtension + ".thumbnail" + media.getThumbnailExtension();

            media.setUrlPath(media.getUrlPathPrefix() + parentDirectory + name);
            media.setPreviewFile(new File(absoluteMediasDirectory + separator + previewPath));
            media.setPreviewUrlPath(getFormatsUrlPathPrefix() + previewPath);
            media.setThumbnailFile(new File(absoluteMediasDirectory + separator + thumbnailPath));
            media.setThumbnailUrlPath(getFormatsUrlPathPrefix() + thumbnailPath);
            media.setup(absoluteMediasDirectory, relativeFile);
            return media;
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }
}
