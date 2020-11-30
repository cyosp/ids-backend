package com.cyosp.ids.service;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;

import static com.cyosp.ids.model.Image.IDS_HIDDEN_DIRECTORY;
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

    public boolean isDirectory(Path path) {
        return path.toFile().isDirectory() && !path.getFileName().toString().equals(IDS_HIDDEN_DIRECTORY);
    }

    private File relative(Path path) {
        return new File(relative(path.toFile().toString()));
    }

    private String relative(String absolutePath) {
        return absolutePath.replaceFirst("^" + idsConfiguration.getAbsoluteImagesDirectory() + separator, "");
    }

    public String stringRelative(Path path) {
        return relative(path.toString());
    }

    public String stringRelative(FileSystemElement fileSystemElement) {
        return stringRelative(Path.of(fileSystemElement.getFile().toURI()));
    }

    public Image imageFrom(Path path) {
        return Image.from(idsConfiguration.getAbsoluteImagesDirectory(), relative(path));
    }

    public Directory directoryFrom(Path path) {
        return new Directory(idsConfiguration.getAbsoluteImagesDirectory(), relative(path));
    }
}
