package com.yt.backend.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.yt.backend.config.AppPaths.location;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppPaths paths;

    public WebConfig(AppPaths paths) {
        this.paths = paths;
    }

    /** Frontend duoc goi API (CORS) va mo WebSocket co cookie (WebSocketConfig) */
    static final List<String> ALLOWED_ORIGINS = List.of(
            "http://192.168.100.122:3000",
            "http://localhost:3000",
            "http://192.168.1.7:3000",
            "http://192.168.100.122:8081");

    /**
     * CORS dang filter chay truoc moi filter khac (giong app.use(cors()) dat truoc rate limit ben Node):
     * response 429 / 503 tra tu RateLimitFilter, LoadSheddingFilter van co header CORS
     * de frontend doc duoc body (vd. CAPTCHA_REQUIRED), preflight OPTIONS khong bi tinh luot.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(List.of("GET", "HEAD", "PUT", "PATCH", "POST", "DELETE"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Trang quan tri (HTML/JS thuan trong src/main/resources/admin, dong goi trong JAR), du lieu lay qua /api/admin/*
        registry.addRedirectViewController("/admin", "/admin/");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Nam trong JAR (classpath) nen khong phu thuoc thu muc server/ cua ban Node.
        // no-cache: sua admin.js / admin.css xong build lai la trinh duyet lay ban moi
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/admin/")
                .setCacheControl(CacheControl.noCache());

        // build/static/* co hash trong ten file nen cache vinh vien duoc
        registry.addResourceHandler("/static/**")
                .addResourceLocations(location(paths.build().resolve("static")))
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

        // Thumbnail / avatar / banner / video: cache 1 ngay thay vi tai lai moi lan dieu huong.
        // server/uploads chua file nguoi dung dang, public/ chua file cu va file tinh.
        for (String dir : new String[]{"video", "thumb", "avatar", "banner"}) {
            registry.addResourceHandler("/" + dir + "/**")
                    .addResourceLocations(
                            location(paths.uploads().resolve(dir)),
                            location(paths.build().resolve(dir)),
                            location(paths.publicDir().resolve(dir)))
                    .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS));
        }

        // Cac file con lai (index.html, manifest...) phai revalidate de khong phuc vu ban cu
        registry.addResourceHandler("/**")
                .addResourceLocations(location(paths.build()), location(paths.publicDir()))
                .setCacheControl(CacheControl.noCache());
    }
}
