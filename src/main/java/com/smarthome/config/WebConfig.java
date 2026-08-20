package com.smarthome.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC 行为配置：把上传的头像目录暴露成可访问的 URL。
 *
 * <p>WebMvcConfigurer 是 Spring MVC 提供的“扩展点”接口：
 * 我们实现它并贴上 @Configuration，DispatcherServlet 初始化时
 * 会把这里的资源映射规则一并注册进去。
 * 效果是：浏览器访问 /avatars/1-ab12cd34.png 时，
 * 实际读取的是项目目录下 uploads/avatars/1-ab12cd34.png。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:uploads/avatars/");
    }
}
