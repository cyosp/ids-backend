package com.cyosp.ids.graphql;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.graphql.exception.BadCredentialsException;
import com.cyosp.ids.graphql.exception.ImageDoesntExistException;
import com.cyosp.ids.graphql.exception.SameFieldsException;
import com.cyosp.ids.graphql.exception.ForbiddenException;
import com.cyosp.ids.model.Directory;
import com.cyosp.ids.model.FileSystemElement;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.ImageMetadata;
import com.cyosp.ids.model.User;
import com.cyosp.ids.repository.UserRepository;
import com.cyosp.ids.service.ModelService;
import com.cyosp.ids.service.PasswordService;
import graphql.GraphQLContext;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.imageio.IIOImage;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.cyosp.ids.model.Role.ADMINISTRATOR;
import static java.awt.Image.SCALE_SMOOTH;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.io.File.separator;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.newDirectoryStream;
import static java.nio.file.Paths.get;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.reverseOrder;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static javax.imageio.ImageIO.createImageOutputStream;
import static javax.imageio.ImageIO.getImageWritersByFormatName;
import static javax.imageio.ImageIO.read;
import static javax.imageio.ImageWriteParam.MODE_EXPLICIT;
import static org.imgscalr.Scalr.resize;
import static org.springframework.security.core.context.SecurityContextHolder.getContext;

@Slf4j
@Component
@RequiredArgsConstructor
public class GraphQLDataFetchers {
    private static final String DIRECTORY = "directory";
    private static final String DIRECTORY_REVERSED_ORDER = "directoryReversedOrder";
    private static final String PREVIEW_DIRECTORY_REVERSED_ORDER = "previewDirectoryReversedOrder";
    private static final String FORCE_THUMBNAIL_GENERATION = "forceThumbnailGeneration";
    private static final String PASSWORD = "password";
    private static final String NEW_PASSWORD = "newPassword";
    private static final String IMAGE = "image";

    private final IdsConfiguration idsConfiguration;

    private final UserRepository userRepository;

    private final ModelService modelService;

    private final PasswordService passwordService;

    private final AuthenticationManagerBuilder authenticationManagerBuilder;

    private final Map<String, Image> previewDirectoryNaturalOrderMap = new HashMap<>();
    private final Map<String, Image> previewDirectoryReversedOrderMap = new HashMap<>();
    private boolean previewDirectoryLoaded = false;

    @EventListener(ApplicationReadyEvent.class)
    public void loadPreviewDirectory() {
        if (idsConfiguration.isStaticPreviewDirectory()) {
            list(null, true, false, false);
            previewDirectoryLoaded = true;
        }
    }

    List<Image> listImagesInAllDirectories(String directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        List<Image> images = new ArrayList<>();
        for (FileSystemElement fileSystemElement : listFileSystemElements(directory, directoryReversedOrder, previewDirectoryReversedOrder)) {
            if (fileSystemElement instanceof Image)
                images.add((Image) fileSystemElement);
            else {
                images.addAll(
                        listRecursively(fileSystemElement.getId(), directoryReversedOrder, previewDirectoryReversedOrder).stream()
                                .filter(Image.class::isInstance)
                                .map(Image.class::cast)
                                .collect(toList()));
            }
        }
        return images;
    }

    List<FileSystemElement> listRecursively(String directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        return list(directory, true, directoryReversedOrder, previewDirectoryReversedOrder);
    }

    List<FileSystemElement> listFileSystemElements(String directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        return list(directory, false, directoryReversedOrder, previewDirectoryReversedOrder);
    }

    List<FileSystemElement> listFileSystemElements(Directory directory, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        return list(modelService.stringRelative(directory), false, directoryReversedOrder, previewDirectoryReversedOrder);
    }

    private List<FileSystemElement> list(String directoryString, boolean recursive, boolean directoryReversedOrder, boolean previewDirectoryReversedOrder) {
        final List<FileSystemElement> fileSystemElements = new ArrayList<>();

        List<Path> unorderedPaths = new ArrayList<>();
        try (DirectoryStream<Path> paths = newDirectoryStream(get(getAbsoluteDirectoryPath(directoryString)),
                path -> modelService.isImage(path) || modelService.isDirectory(path))) {
            paths.forEach(unorderedPaths::add);
        } catch (IOException e) {
            log.warn("Fail to list file system elements: " + e.getMessage());
        }
        unorderedPaths.stream()
                .filter(modelService::isDirectory)
                .sorted(comparing(path -> path.getFileName().toString()))
                .sorted(directoryReversedOrder ? reverseOrder() : naturalOrder())
                .forEach(path -> {
                    Directory directory = modelService.directoryFrom(path);
                    Image preview = null;
                    if (idsConfiguration.isStaticPreviewDirectory()) {
                        String directoryId = directory.getId();
                        if (previewDirectoryLoaded) {
                            preview = (previewDirectoryReversedOrder ? previewDirectoryReversedOrderMap
                                    : previewDirectoryNaturalOrderMap).get(directoryId);
                        } else {
                            // TODO Avoid to iterate twice
                            previewDirectoryNaturalOrderMap.put(directoryId,  preview(directory, false));
                            previewDirectoryReversedOrderMap.put(directoryId,  preview(directory, true));
                        }
                    } else {
                        preview = preview(directory, previewDirectoryReversedOrder);
                    }
                    directory.setPreview(preview);
                    fileSystemElements.add(directory);
                    if (recursive) {
                        fileSystemElements.addAll(listRecursively(modelService.stringRelative(path), directoryReversedOrder, previewDirectoryReversedOrder));
                    }
                });
        unorderedPaths.stream()
                .filter(modelService::isImage)
                .sorted(comparing(path -> path.getFileName().toString()))
                .forEach(path -> fileSystemElements.add(modelService.imageFrom(path)));

        return fileSystemElements;
    }

    private String getAbsoluteDirectoryPath(String relativeDirectory) {
        final StringBuilder absoluteDirectoryPath = new StringBuilder(idsConfiguration.getAbsoluteImagesDirectory());
        if (ofNullable(relativeDirectory).isPresent())
            absoluteDirectoryPath.append(separator).append(relativeDirectory);
        return absoluteDirectoryPath.toString();
    }

    Image preview(Directory directory, boolean previewDirectoryReversedOrder) {
        List<FileSystemElement> fileSystemElements = listFileSystemElements(directory, previewDirectoryReversedOrder, previewDirectoryReversedOrder);

        Image image = fileSystemElements.stream()
                .map(fse -> Path.of(fse.getFile().toURI()))
                .filter(modelService::isImage)
                .sorted(previewDirectoryReversedOrder ? reverseOrder() : naturalOrder())
                .map(modelService::imageFrom)
                .findFirst()
                .orElse(null);

        return ofNullable(image)
                .orElse(fileSystemElements.stream()
                        .map(fse -> Path.of(fse.getFile().toURI()))
                        .filter(modelService::isDirectory)
                        .map(modelService::directoryFrom)
                        .map(dir -> preview(dir, previewDirectoryReversedOrder))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null));
    }

    public DataFetcher<List<FileSystemElement>> getFileSystemElementsDataFetcher() {
        return dataFetchingEnvironment -> {
            String directory = dataFetchingEnvironment.getArgument(DIRECTORY);

            GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();

            boolean directoryReversedOrder = TRUE.equals(dataFetchingEnvironment.getArgument(DIRECTORY_REVERSED_ORDER));
            graphQLContext.put(DIRECTORY_REVERSED_ORDER, directoryReversedOrder);

            boolean previewDirectoryReversedOrder = TRUE.equals(dataFetchingEnvironment.getArgument(PREVIEW_DIRECTORY_REVERSED_ORDER));
            graphQLContext.put(PREVIEW_DIRECTORY_REVERSED_ORDER, previewDirectoryReversedOrder);

            return new ArrayList<>(listFileSystemElements(directory, directoryReversedOrder, previewDirectoryReversedOrder));
        };
    }

    public DataFetcher<Image> getImage() {
        return this::getImage;
    }

    public DataFetcher<List<Image>> getImages() {
        return dataFetchingEnvironment -> {
            Iterator<Path> pathIterator = Files.list(get(getAbsoluteDirectoryPath(dataFetchingEnvironment.getArgument(DIRECTORY))))
                    .filter(modelService::isImage)
                    .sorted(comparing(path -> path.getFileName().toString()))
                    .iterator();

            String image = dataFetchingEnvironment.getArgument(IMAGE);
            Path currentPath = null;
            while (pathIterator.hasNext()) {
                Path previousPath = currentPath;
                currentPath = pathIterator.next();
                if (currentPath.getFileName().toString().equals(image)) {
                    return asList(nonNull(previousPath) ? modelService.imageFrom(previousPath) : null,
                            modelService.imageFrom(currentPath),
                            pathIterator.hasNext() ? modelService.imageFrom(pathIterator.next()) : null);
                }
            }

            return asList(null, null, null);
        };
    }

    public DataFetcher<ImageMetadata> getImageMetadata() {
        return dataFetchingEnvironment -> ImageMetadata.from(dataFetchingEnvironment.getSource());
    }

    public DataFetcher<List<FileSystemElement>> getDirectoryElementsDataFetcher() {
        return dataFetchingEnvironment -> {
            Directory directory = dataFetchingEnvironment.getSource();

            GraphQLContext graphQLContext = dataFetchingEnvironment.getContext();

            boolean directoryReversedOrder = TRUE.equals(graphQLContext.get(DIRECTORY_REVERSED_ORDER));
            boolean previewDirectoryReversedOrder = TRUE.equals(graphQLContext.get(PREVIEW_DIRECTORY_REVERSED_ORDER));

            return new ArrayList<>(listFileSystemElements(directory.getId(), directoryReversedOrder, previewDirectoryReversedOrder));
        };
    }

    void checkAdministratorUser() throws AccessDeniedException {
        if (getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(authority -> ADMINISTRATOR.name().equals(authority)))
            throw new AccessDeniedException("Only administrator user is allowed");
    }

    public DataFetcher<List<User>> getUsersDataFetcher() {
        return dataFetchingEnvironment -> {
            checkAdministratorUser();
            return userRepository.findAll();
        };
    }

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
            checkAdministratorUser();
            // Generate alternative formats first for recent dated folders
            final boolean directoryReversedOrder = true;
            final boolean previewDirectoryReversedOrder = false;
            boolean forceThumbnailGeneration = TRUE.equals(dataFetchingEnvironment.getArgument(FORCE_THUMBNAIL_GENERATION));
            final List<Image> images = new ArrayList<>();
            String directory = dataFetchingEnvironment.getArgument(DIRECTORY);
            for (Image image : listImagesInAllDirectories(directory, directoryReversedOrder, previewDirectoryReversedOrder)) {
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
            if(idsConfiguration.isPasswordChangeAllowed()) {
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
            checkAdministratorUser();

            Image image = getImage(dataFetchingEnvironment);
            deleteImage(image.getThumbnailFile(), false);
            deleteImage(image.getPreviewFile(), false);
            deleteImage(image.getFile(), true);

            return image;
        };
    }

    private Image getImage(DataFetchingEnvironment dataFetchingEnvironment) {
        String imageId = dataFetchingEnvironment.getArgument(IMAGE).toString();
        Path absoluteImagePath = Paths.get(idsConfiguration.getAbsoluteImagesDirectory(), separator, imageId);
        return modelService.imageFrom(absoluteImagePath);
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
