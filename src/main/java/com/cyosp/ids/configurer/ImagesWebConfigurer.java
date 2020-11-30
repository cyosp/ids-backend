package com.cyosp.ids.configurer;

import com.cyosp.ids.configuration.IdsConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static com.cyosp.ids.model.Image.IMAGES_URL_PATH;

@Configuration
public class ImagesWebConfigurer implements WebMvcConfigurer {

    private final IdsConfiguration idsConfiguration;

    public ImagesWebConfigurer(IdsConfiguration idsConfiguration) {
        this.idsConfiguration = idsConfiguration;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry resourceHandlerRegistry) {
        resourceHandlerRegistry.addResourceHandler(IMAGES_URL_PATH + "/**")
                .addResourceLocations("file:" + idsConfiguration.getAbsoluteImagesDirectory() + "/");
    }
}