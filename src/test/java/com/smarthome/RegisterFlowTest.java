package com.smarthome;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 注册流程的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegisterFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registerPageLoads() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("注册账号")));
    }

    @Test
    void mismatchedPasswordsRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "user_abc")
                        .param("password", "abc123")
                        .param("confirmPassword", "abc124"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("两次输入的密码不一致")));
    }

    @Test
    void invalidUsernameRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "ab")   // 少于 4 位
                        .param("password", "abc123")
                        .param("confirmPassword", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("账号需为 4-20 位")));
    }

    @Test
    void duplicateUsernameRejected() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "admin")   // 已被演示账号占用
                        .param("password", "abc123")
                        .param("confirmPassword", "abc123"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("用户名已存在")));
    }

    @Test
    void validRegistrationThenLogin() throws Exception {
        // 注册成功 -> 跳回登录页
        mockMvc.perform(post("/register")
                        .param("username", "newuser01")
                        .param("password", "abc123")
                        .param("confirmPassword", "abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        // 用新账号能登录
        MvcResult login = mockMvc.perform(post("/login")
                        .param("username", "newuser01")
                        .param("password", "abc123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(cookie().exists("SESSION"))
                .andReturn();

        // 登录后能访问个人页
        Cookie session = login.getResponse().getCookie("SESSION");
        mockMvc.perform(get("/profile").cookie(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("newuser01")));
    }
}
