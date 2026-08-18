package com.smarthome;

import java.util.Optional;

import com.smarthome.model.User;
import com.smarthome.service.AuthService;
import com.smarthome.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 登录 / 登出 / 会话缓存的单元测试。
 *
 * <p>@SpringBootTest 会启动一个真实的 Spring 容器，
 * @Autowired 表示“请容器把 bean 注入到我的测试字段里”。
 * 这本身就是 IoC 最直观的体现。
 */
@SpringBootTest
class AuthServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @Test
    void registerThenLoginThenLogout() {
        String username = "tester";
        userService.register(username, "pass123", "测试用户", "t@smarthome.com", "bio");

        // 密码正确 -> 拿到 token
        Optional<String> token = authService.login(username, "pass123");
        assertTrue(token.isPresent());

        // 用 token 能取回当前用户
        Optional<User> current = authService.currentUser(token.get());
        assertTrue(current.isPresent());
        assertEquals(username, current.get().getUsername());

        // 登出后 token 失效
        authService.logout(token.get());
        assertTrue(authService.currentUser(token.get()).isEmpty());
    }

    @Test
    void wrongPasswordFails() {
        userService.register("wrongpass", "right", "n", "e", "b");
        assertTrue(authService.login("wrongpass", "wrong").isEmpty());
    }
}
