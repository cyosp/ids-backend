package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.Media;
import com.cyosp.ids.model.Video;
import graphql.schema.DataFetchingEnvironment;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.cyosp.ids.graphql.GraphQLProvider.MEDIA;
import static java.io.File.separator;

@Service
public class ModelService {

    private final IdsConfiguration idsConfiguration;

    public ModelService(IdsConfiguration idsConfiguration) {
        this.idsConfiguration = idsConfiguration;
    }

    String lowerCaseExtension(Path path) {
        String fileName = path.getFileName().toString();
        String extension = "";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = fileName.substring(dotIndex).toLowerCase();
        }
        return extension;
    }

    public boolean isImage(Path path) {
        return lowerCaseExtension(path).endsWith(".jpg");
    }

    public boolean isVideo(Path path) {
        return lowerCaseExtension(path).endsWith(".mov");
    }

    public boolean isMedia(Path path) {
        return isImage(path) || isVideo(path);
    }

    public boolean isDirectory(Path path) {
        File file = path.toFile();
        return file.isDirectory() && !file.isHidden();
    }

    private File relative(Path path) {
        return new File(relative(path.toFile().toString()));
    }

    private String relative(String absolutePath) {
        return absolutePath.replaceFirst("^" + idsConfiguration.getAbsoluteMediasDirectory() + separator, "");
    }

    public String stringRelative(Path path) {
        return relative(path.toString());
    }

    public String stringRelative(FileSystemElement fileSystemElement) {
        return stringRelative(Path.of(fileSystemElement.getFile().toURI()));
    }

    public Media mediaFrom(Path path) {
        File relativePath = relative(path);
        if (isImage(path)) {
            return Image.from(idsConfiguration.getAbsoluteMediasDirectory(), relativePath);
        } else if (isVideo(path)) {
            return Video.from(idsConfiguration.getAbsoluteMediasDirectory(), relativePath);
        } else {
            throw new NotImplementedException("No media match for: " + relativePath);
        }
    }

    public Directory directoryFrom(Path path) {
        return new Directory(idsConfiguration.getAbsoluteMediasDirectory(), relative(path));
    }

    public Media getMedia(DataFetchingEnvironment dataFetchingEnvironment) {
        String mediaId = dataFetchingEnvironment.getArgument(MEDIA).toString();
        Path absoluteMediaPath = Paths.get(idsConfiguration.getAbsoluteMediasDirectory(), separator, mediaId);
        return mediaFrom(absoluteMediaPath);
    }
}
