package com.cyosp.ids.configurer;

import com.cyosp.ids.configuration.IdsConfiguration;
import com.cyosp.ids.model.Image;
import com.cyosp.ids.model.Video;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.cyosp.ids.model.Media.getFormatsUrlPathPrefix;
import static com.cyosp.ids.model.Media.getUrlPathPrefix;
import static java.io.File.separator;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.springframework.http.CacheControl.maxAge;

@Configuration
@RequiredArgsConstructor
public class ImagesWebConfigurer implements WebMvcConfigurer {
    private final IdsConfiguration idsConfiguration;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {
        final String resourceLocation = "file:" + idsConfiguration.getAbsoluteMediasDirectory() + separator;

        resourceHandlerRegistry.addResourceHandler(getFormatsUrlPathPrefix() + "**")
                .addResourceLocations(resourceLocation)
                .setCacheControl(maxAge(90, DAYS));

        resourceHandlerRegistry.addResourceHandler(getUrlPathPrefix(Image.class) + "**")
                .addResourceLocations(resourceLocation)
                .setCacheControl(maxAge(10, MINUTES));

        resourceHandlerRegistry.addResourceHandler(getUrlPathPrefix(Video.class) + "**")
                .addResourceLocations(resourceLocation)
                .setCacheControl(maxAge(10, MINUTES));
    }
}
