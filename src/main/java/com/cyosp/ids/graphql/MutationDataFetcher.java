package com.cyosp.ids.graphql;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.graphql.exception.BadCredentialsException;
import com.cyosp.ids.graphql.exception.ForbiddenException;
import com.cyosp.ids.graphql.exception.ImageDoesntExistException;
import com.cyosp.ids.graphql.exception.SameFieldsException;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
import com.cyosp.ids.service.FileSystemElementService;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.PasswordService;
import com.cyosp.ids.service.SecurityService;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.cyosp.ids.graphql.GraphQLProvider.DIRECTORY;
import static com.cyosp.ids.graphql.GraphQLProvider.FORCE_THUMBNAIL_GENERATION;
import static com.cyosp.ids.graphql.GraphQLProvider.NEW_PASSWORD;
import static com.cyosp.ids.graphql.GraphQLProvider.PASSWORD;
import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.io.File.separator;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Paths.get;
import static javax.imageio.ImageIO.createImageOutputStream;
import static javax.imageio.ImageIO.getImageWritersByFormatName;
import static javax.imageio.ImageIO.read;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;
import static org.imgscalr.Scalr.resize;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
@RequiredArgsConstructor
public class MutationDataFetcher {
    private final IdsConfiguration idsConfiguration;

    private final UserRepository userRepository;

    private final ModelService modelService;

    private final PasswordService passwordService;

    private final FileSystemElementService fileSystemElementService;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final SecurityService securityService;

    BufferedImage createPreview(BufferedImage bufferedImage) {
        int previewImageWidth;
        int previewImageHeight;
        final int previewMaximumSize = 1080;
        final float previewImageRatio = (float) bufferedImage.getWidth() / bufferedImage.getHeight();
        if (bufferedImage.getWidth() >= bufferedImage.getHeight()) {
            previewImageWidth = previewMaximumSize;
            previewImageHeight = (int) (previewImageWidth * previewImageRatio);
        } else {
            previewImageHeight = previewMaximumSize;
            previewImageWidth = (int) (previewImageHeight * previewImageRatio);
        }
        return resize(bufferedImage, previewImageWidth, previewImageHeight);
    }

    BufferedImage createThumbnail(BufferedImage bufferedImage) {
        int imageWidth = bufferedImage.getWidth();
        int imageHeight = bufferedImage.getHeight();

        boolean portrait = imageHeight > imageWidth;

        BufferedImage croppedImage;
        if (portrait) {
            int startCrop = (imageHeight - imageWidth) / 2;
            croppedImage = bufferedImage.getSubimage(0, startCrop, imageWidth, imageWidth);
        } else {
            int startCrop = (imageWidth - imageHeight) / 2;
            croppedImage = bufferedImage.getSubimage(startCrop, 0, imageHeight, imageHeight);
        }

        final int squareSize = 200;
        java.awt.Image squareImage = croppedImage.getScaledInstance(squareSize, squareSize, SCALE_SMOOTH);
        BufferedImage thumbnailImage = new BufferedImage(squareSize, squareSize, TYPE_INT_RGB);
        thumbnailImage.getGraphics().drawImage(squareImage, 0, 0, null);
        return thumbnailImage;
    }

    void save(BufferedImage bufferedImage, File file) throws IOException {
        createDirectories(get(file.getParent()));

        String filename = file.getName();

        int dotIndex = filename.lastIndexOf('.');
        String extension = dotIndex > 0 && dotIndex < filename.length() ? filename.substring(dotIndex + 1) : null;

        String jpgFormat = "jpg";
        if (!jpgFormat.equalsIgnoreCase(extension))
            throw new UnsupportedOperationException(format("Only %s format is managed", jpgFormat));

        ImageWriter imageWriter = getImageWritersByFormatName(jpgFormat).next();

        FileOutputStream fileOutputStream = new FileOutputStream(file);
        ImageOutputStream imageOutputStream = createImageOutputStream(fileOutputStream);
        imageWriter.setOutput(imageOutputStream);

        ImageWriteParam imageWriteParam = imageWriter.getDefaultWriteParam();
        imageWriteParam.setCompressionMode(MODE_EXPLICIT);
        imageWriteParam.setCompressionQuality(0.9f);

        imageWriter.write(null, new IIOImage(bufferedImage, null, null), imageWriteParam);

        imageOutputStream.close();
        fileOutputStream.close();
        imageWriter.dispose();
    }

    public DataFetcher<List<Image>> generateAlternativeFormats() {
        return dataFetchingEnvironment -> {
            securityService.checkAdministratorUser();
            // Generate alternative formats first for recent dated folders
            final boolean directoryReversedOrder = true;
            final boolean previewDirectoryReversedOrder = false;
            boolean forceThumbnailGeneration = TRUE.equals(dataFetchingEnvironment.getArgument(FORCE_THUMBNAIL_GENERATION));
            final List<Image> images = new ArrayList<>();
            String directory = dataFetchingEnvironment.getArgument(DIRECTORY);
            for (Image image : fileSystemElementService.listImagesInAllDirectories(directory, directoryReversedOrder, previewDirectoryReversedOrder)) {
                File previewFile = image.getPreviewFile();
                File thumbnailFile = image.getThumbnailFile();

                if (!previewFile.exists() || !thumbnailFile.exists() || forceThumbnailGeneration) {
                    BufferedImage bufferedImage = read(image.getFile());

                    if (!previewFile.exists()) {
                        log.info("Create: {}", previewFile.getAbsolutePath());
                        save(createPreview(bufferedImage), previewFile);
                    }

                    if (!thumbnailFile.exists() || forceThumbnailGeneration) {
                        log.info("Create: {}", thumbnailFile.getAbsolutePath());
                        save(createThumbnail(bufferedImage), thumbnailFile);
                    }

                    images.add(image);
                }
            }
            return images;
        };
    }

    public DataFetcher<User> changePassword() {
        return dataFetchingEnvironment -> {
            if (idsConfiguration.isPasswordChangeAllowed()) {
                String password = dataFetchingEnvironment.getArgument(PASSWORD);
                authenticateTokenizedUserWith(password);

                String newPassword = dataFetchingEnvironment.getArgument(NEW_PASSWORD);
                if (password.equals(newPassword)) {
                    throw new SameFieldsException("Passwords are same");
                }

                User user = userRepository.getByEmail(getContext().getAuthentication().getName());
                user.setPassword(newPassword);
                user.setHashedPassword(passwordService.encode(user.getPassword()));
                return userRepository.save(user);
            } else {
                throw new ForbiddenException("Password change not allowed");
            }
        };
    }

    public DataFetcher<Image> deleteImage() {
        return dataFetchingEnvironment -> {
            securityService.checkAdministratorUser();

            Image image = modelService.getImage(dataFetchingEnvironment);
            deleteImage(image.getThumbnailFile(), false);
            deleteImage(image.getPreviewFile(), false);
            deleteImage(image.getFile(), true);

            return image;
        };
    }

    private void deleteImage(File image, boolean imageMustExist) throws IOException {
        String imageAbsolutePath = image.getAbsolutePath();
        log.info("Image to delete: {}", imageAbsolutePath);

        if (image.exists()) {
            delete(image.toPath());
        } else if (imageMustExist) {
            String imageId = image.getAbsoluteFile().getAbsolutePath()
                    .replace(idsConfiguration.getAbsoluteImagesDirectory() + separator, "");
            throw new ImageDoesntExistException(imageId);
        } else {
            log.info("Image doesn't exist");
        }
    }

    private void authenticateTokenizedUserWith(String password) {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                new UsernamePasswordAuthenticationToken(getContext().getAuthentication().getName(), password);
        try {
            authenticationManagerBuilder.getObject().authenticate(usernamePasswordAuthenticationToken);
        } catch (Exception e) {
            throw new BadCredentialsException(e.getMessage());
        }
    }
}
