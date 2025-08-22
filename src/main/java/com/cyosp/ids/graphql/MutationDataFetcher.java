package com.cyosp.ids.graphql;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.graphql.exception.BadCredentialsException;
import com.cyosp.ids.graphql.exception.ForbiddenException;
import com.cyosp.ids.graphql.exception.MediaDoesntExistException;
import com.cyosp.ids.graphql.exception.SameFieldsException;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.Media;
import com.cyosp.ids.model.User;
import com.cyosp.ids.model.Video;
import com.cyosp.ids.repository.UserRepository;
import com.cyosp.ids.service.FileSystemElementService;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.PasswordService;
import com.cyosp.ids.service.SecurityService;
import graphql.schema.DataFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.stereotype.Component;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;
import ws.schild.jave.encode.VideoAttributes;

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
import java.util.Optional;

import static com.cyosp.ids.graphql.GraphQLProvider.DIRECTORY;
import static com.cyosp.ids.graphql.GraphQLProvider.FORCE_THUMBNAIL_GENERATION;
import static com.cyosp.ids.graphql.GraphQLProvider.NEW_PASSWORD;
import static com.cyosp.ids.graphql.GraphQLProvider.PASSWORD;
import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.io.File.separator;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Paths.get;
import static java.util.Optional.empty;
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

    private Optional<BufferedImage> createPreviewImage(BufferedImage bufferedImage) {
        int previewImageWidth;
        int previewImageHeight;
        final int previewMaximumSize = 1080;
        int imageWidth = bufferedImage.getWidth();
        int imageHeight = bufferedImage.getHeight();
        final float previewImageRatio = (float) imageWidth / imageHeight;
        if (imageWidth >= imageHeight && imageWidth > previewMaximumSize) {
            previewImageWidth = previewMaximumSize;
            previewImageHeight = (int) (previewImageWidth / previewImageRatio);
        } else if (imageHeight > imageWidth && imageHeight > previewMaximumSize) {
            previewImageHeight = previewMaximumSize;
            previewImageWidth = (int) (previewImageHeight / previewImageRatio);
        } else {
            return empty();
        }
        return Optional.of(resize(bufferedImage, previewImageWidth, previewImageHeight));
    }

    private void copyFileToPreview(Media media) throws IOException {
        File previewFile = media.getPreviewFile();
        createDirectories(get(previewFile.getParent()));
        copy(media.getFile().toPath(), previewFile.toPath());
    }

    private BufferedImage createThumbnailImage(BufferedImage bufferedImage) {
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

    private void save(BufferedImage bufferedImage, File file) throws IOException {
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

    private void createPreviewVideo(Video source, File output) {
        try {
            AudioAttributes audioAttributes = new AudioAttributes();
            audioAttributes.setCodec("libmp3lame");
            audioAttributes.setBitRate(180_000);
            VideoAttributes videoAttributes = new VideoAttributes();
            videoAttributes.setCodec("libx264"); // 2025-08-19: mpeg4 coded is not supported by Firefox
            videoAttributes.setBitRate(1_200_000);
            EncodingAttributes encodingAttributes = new EncodingAttributes();
            encodingAttributes.setOutputFormat("mp4");
            encodingAttributes.setAudioAttributes(audioAttributes);
            encodingAttributes.setVideoAttributes(videoAttributes);
            new Encoder().encode(new MultimediaObject(source.getFile()), output, encodingAttributes);
        } catch (Exception e) {
            log.error("Fail to create preview video: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private BufferedImage extractImage(Video video) {
        try (FFmpegFrameGrabber ffmpegFrameGrabber = new FFmpegFrameGrabber(video.getFile());
             Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter()) {
            ffmpegFrameGrabber.start();

            int totalFrames = ffmpegFrameGrabber.getLengthInFrames();
            double frameRate = ffmpegFrameGrabber.getFrameRate();
            double videoDurationInSecond = totalFrames / frameRate;

            int expectedTimeToExtractImageInSecond = 3;
            int timeToExtractImageInSecond = videoDurationInSecond > expectedTimeToExtractImageInSecond ? expectedTimeToExtractImageInSecond : (int) (videoDurationInSecond / 2);
            int frameToExtract = timeToExtractImageInSecond * (int) frameRate;

            for (int i = 1; i < frameToExtract; i++) {
                ffmpegFrameGrabber.grabImage();
            }
            Frame frame = ffmpegFrameGrabber.grabImage();
            ffmpegFrameGrabber.stop();
            return java2DFrameConverter.convert(frame);
        } catch (Exception e) {
            log.error("Fail to extract image: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public DataFetcher<List<Media>> generateAlternativeFormats() {
        return dataFetchingEnvironment -> {
            securityService.checkAdministratorUser();
            // Generate alternative formats first for recent dated folders
            final boolean directoryReversedOrder = true;
            final boolean previewDirectoryReversedOrder = false;
            boolean forceThumbnailGeneration = TRUE.equals(dataFetchingEnvironment.getArgument(FORCE_THUMBNAIL_GENERATION));
            final List<Media> medias = new ArrayList<>();
            String directory = dataFetchingEnvironment.getArgument(DIRECTORY);
            for (Media media : fileSystemElementService.listMediasInAllDirectories(directory, directoryReversedOrder, previewDirectoryReversedOrder)) {
                File previewFile = media.getPreviewFile();
                File thumbnailFile = media.getThumbnailFile();

                if (!previewFile.exists() || !thumbnailFile.exists() || forceThumbnailGeneration) {
                    if (media instanceof Image) {
                        BufferedImage bufferedImage = read(media.getFile());

                        if (!previewFile.exists()) {
                            log.info("Create: {}", previewFile.getAbsolutePath());
                            Optional<BufferedImage> optionalBufferedImage = createPreviewImage(bufferedImage);
                            if (optionalBufferedImage.isPresent()) {
                                save(optionalBufferedImage.get(), previewFile);
                            } else {
                                copyFileToPreview(media);
                            }
                        }

                        if (!thumbnailFile.exists() || forceThumbnailGeneration) {
                            log.info("Create: {}", thumbnailFile.getAbsolutePath());
                            save(createThumbnailImage(bufferedImage), thumbnailFile);
                        }
                    } else if (media instanceof Video) {
                        Video video = (Video) media;
                        if (!previewFile.exists()) {
                            log.info("Create: {}", previewFile.getAbsolutePath());
                            createPreviewVideo(video, previewFile);
                        }

                        if (!thumbnailFile.exists() || forceThumbnailGeneration) {
                            log.info("Create: {}", thumbnailFile.getAbsolutePath());
                            save(createThumbnailImage(extractImage(video)), thumbnailFile);
                        }
                    }
                    medias.add(media);
                }
            }
            return medias;
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

    public DataFetcher<Media> deleteMedia() {
        return dataFetchingEnvironment -> {
            securityService.checkAdministratorUser();

            Media media = modelService.getMedia(dataFetchingEnvironment);
            deleteMedia(media.getThumbnailFile(), false);
            deleteMedia(media.getPreviewFile(), false);
            deleteMedia(media.getFile(), true);

            return media;
        };
    }

    private void deleteMedia(File media, boolean mediaMustExist) throws IOException {
        String mediaAbsolutePath = media.getAbsolutePath();
        log.info("Media to delete: {}", mediaAbsolutePath);

        if (media.exists()) {
            delete(media.toPath());
        } else if (mediaMustExist) {
            String mediaId = media.getAbsoluteFile().getAbsolutePath()
                    .replace(idsConfiguration.getAbsoluteMediasDirectory() + separator, "");
            throw new MediaDoesntExistException(mediaId);
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
