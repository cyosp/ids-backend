package com.cyosp.ids.configurer;

import com.cyosp.ids.configuration.IdsConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.cyosp.ids.model.Image.FORMATS_URL_PATH_PREFIX;
import static com.cyosp.ids.model.Image.IMAGES_URL_PATH_PREFIX;
import static java.io.File.separator;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.springframework.http.CacheControl.maxAge;

@Configuration
public class ImagesWebConfigurer implements WebMvcConfigurer {

    private final IdsConfiguration idsConfiguration;

    public ImagesWebConfigurer(IdsConfiguration idsConfiguration) {
        this.idsConfiguration = idsConfiguration;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {
        final String resourceLocation = "file:" + idsConfiguration.getAbsoluteImagesDirectory() + separator;

        resourceHandlerRegistry.addResourceHandler(FORMATS_URL_PATH_PREFIX + "**")
                .addResourceLocations(resourceLocation)
                .setCacheControl(maxAge(90, DAYS));

        resourceHandlerRegistry.addResourceHandler(IMAGES_URL_PATH_PREFIX + "**")
                .addResourceLocations(resourceLocation)
                .setCacheControl(maxAge(10, MINUTES));
    }
}
