package com.ppip.dallyeo.config;

import com.ppip.dallyeo.auth.AuthUserArgumentResolver;
import com.ppip.dallyeo.common.storage.StorageProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;
import java.util.List;

/**
 * Web MVC 설정 — {@code @AuthUser} 파라미터 리졸버 등록 (US-AUTH-4),
 * 업로드 이미지 정적 서빙 배선.
 *
 * <p>코스 이미지는 classpath {@code static/images/**}라 Spring Boot 기본 핸들러가 처리한다.
 * 업로드 이미지는 배포마다 교체되는 JAR 바깥 경로라서 파일시스템 핸들러를 별도 등록한다.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class WebConfig implements WebMvcConfigurer {

    private final AuthUserArgumentResolver authUserArgumentResolver;
    private final StorageProperties storageProperties;

    public WebConfig(AuthUserArgumentResolver authUserArgumentResolver,
                     StorageProperties storageProperties) {
        this.authUserArgumentResolver = authUserArgumentResolver;
        this.storageProperties = storageProperties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authUserArgumentResolver);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(storageProperties.dir()).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(storageProperties.publicPath() + "/**")
                .addResourceLocations(location);
    }
}
