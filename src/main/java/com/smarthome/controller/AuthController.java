package com.smarthome.controller;

import java.util.Optional;

import com.smarthome.exception.LoginLockedException;
import com.smarthome.service.AuthService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 处理登录 / 登出。
 *
 * <p>注意这里用的是 @Controller（返回页面）而不是之前的 @RestController（返回 JSON）。
 * @Controller 的方法返回的是一个“视图名”（如 "login"），
 * Spring 会配合 Thymeleaf 去 templates 目录找对应的 HTML 渲染成网页。
 */
@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 展示登录页。 */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /** 提交登录表单。 */
    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpServletResponse response,
                          Model model) {
        Optional<String> token;
        try {
            token = authService.login(username, password);
        } catch (LoginLockedException e) {
            // 防暴力破解：账号被锁定时，把剩余等待时间提示给用户
            model.addAttribute("error", e.getMessage());
            return "login";
        }
        if (token.isEmpty()) {
            model.addAttribute("error", "账号或密码错误");
            return "login";
        }
        // 登录成功：把 token 写进 Cookie，之后每次请求浏览器都会带上它。
        Cookie cookie = new Cookie("SESSION", token.get());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/profile";
    }

    /** 登出：清除会话，回到登录页。 */
    @GetMapping("/logout")
    public String logout(@RequestParam(value = "token", required = false) String token,
                         HttpServletResponse response) {
        if (token != null) {
            authService.logout(token);
        }
        Cookie cookie = new Cookie("SESSION", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/login";
    }
}
