package com.smarthome.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 行为配置：注册登录拦截器 + 头像静态资源映射。
 *
 * <p>WebMvcConfigurer 是 Spring MVC 提供的“扩展点”接口：
 * 我们实现它并贴上 @Configuration，DispatcherServlet 初始化时
 * 会把这里的规则一并注册进去。
 *
 * <p>两件事：
 * 1. addInterceptors：注册登录拦截器，拦截所有 /api/**，
 *    但放行 /api/login 和 /api/register（未登录也能访问的接口）；
 * 2. addResourceHandlers：把上传的头像目录暴露成可访问 URL。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                // 白名单：登录、注册、健康检查不需要鉴权
                .excludePathPatterns("/api/login", "/api/register", "/api/ping");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:uploads/avatars/");
    }
}
