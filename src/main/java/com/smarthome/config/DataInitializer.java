package com.smarthome.config;

import com.smarthome.repository.UserRepository;
import com.smarthome.service.UserService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化一个演示账号，方便直接登录测试。
 *
 * <p>CommandLineRunner 是 Spring Boot 提供的钩子：应用启动完成后、
 * 所有 bean 都创建好之后，会调用它的 run 方法。这里用它来“造一点初始数据”。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    public DataInitializer(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByUsername("admin")) {
            return;
        }
        userService.register("admin", "123456", "管理员", "admin@smarthome.com", "你好，我是智能家居管理员");
    }
}
