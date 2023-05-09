package com.cyosp.ids.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.File;

import static lombok.AccessLevel.PRIVATE;

@Data
@ToString(callSuper = true)
@NoArgsConstructor(access = PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class Directory extends FileSystemElement {

    private Image preview = null;

    public Directory(String absoluteImagesDirectory, File relativeFile) {
        super(absoluteImagesDirectory, relativeFile);
    }
}
