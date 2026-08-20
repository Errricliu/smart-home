package com.smarthome.config;

import java.time.LocalDate;

import com.smarthome.dto.ProfileForm;
import com.smarthome.model.Device;
import com.smarthome.model.Gender;
import com.smarthome.model.Role;
import com.smarthome.model.User;
import com.smarthome.repository.UserRepository;
import com.smarthome.service.DeviceService;
import com.smarthome.service.UserService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化演示数据。
 *
 * <p>CommandLineRunner 是 Spring Boot 的钩子：容器里所有 bean 就绪后
 * 才会执行 run 方法，所以这里可以放心地依赖别的 bean。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final UserService userService;
    private final DeviceService deviceService;

    public DataInitializer(UserRepository userRepository,
                           UserService userService,
                           DeviceService deviceService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.deviceService = deviceService;
    }

    @Override
    public void run(String... args) {
        User admin = userRepository.findByUsername("admin").orElse(null);

        if (admin == null) {
            // 第一次启动：注册 admin 账号，设为管理员，补全资料
            admin = userService.register("admin", "123456");
            admin.setRole(Role.ADMIN);
            userService.updateProfile(admin.getId(), new ProfileForm(
                    "管理员", "张三", Gender.MALE,
                    LocalDate.of(2000, 1, 1),
                    "13800138000", "admin@smarthome.com",
                    "智能家居演示账号"));
        } else if (admin.getRole() != Role.ADMIN) {
            // 老账号升级为管理员
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
        }

        // 确保 admin 名下有几台演示设备（已有则跳过，避免重复添加）
        if (deviceService.listByOwner(admin.getId()).isEmpty()) {
            deviceService.addDevice(admin.getId(), "客厅灯", Device.DeviceType.LIGHT);
            deviceService.addDevice(admin.getId(), "卧室空调", Device.DeviceType.AC);
            deviceService.addDevice(admin.getId(), "客厅电视", Device.DeviceType.TV);
        }
    }
}
