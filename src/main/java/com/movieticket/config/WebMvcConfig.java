package com.movieticket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/movies").setViewName("forward:/movies.html");
        registry.addViewController("/theatres").setViewName("forward:/theatre-selection.html");
        registry.addViewController("/seat-selection").setViewName("forward:/seat-selection.html");
        registry.addViewController("/my-bookings").setViewName("forward:/my-bookings.html");
        registry.addViewController("/login").setViewName("forward:/login.html");
        registry.addViewController("/register").setViewName("forward:/register.html");
        registry.addViewController("/admin").setViewName("forward:/admin.html");
    }
}
