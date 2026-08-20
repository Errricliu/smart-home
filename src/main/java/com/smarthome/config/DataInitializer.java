package com.smarthome.config;

import java.time.LocalDate;

import com.smarthome.dto.ProfileForm;
import com.smarthome.model.Gender;
import com.smarthome.model.User;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.UserService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化一个演示账号，方便直接登录测试。
 *
 * <p>CommandLineRunner 是 Spring Boot 的钩子：容器里所有 bean 就绪后
 * 才会执行 run 方法，所以这里可以放心地依赖别的 bean。
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
        // 先注册账号，再补全个人资料（和用户在网页上的操作顺序一致）
        User admin = userService.register("admin", "123456");
        userService.updateProfile(admin.getId(), new ProfileForm(
                "管理员", "张三", Gender.MALE,
                LocalDate.of(2000, 1, 1),
                "13800138000", "admin@smarthome.com",
                "智能家居演示账号"));
    }
}
