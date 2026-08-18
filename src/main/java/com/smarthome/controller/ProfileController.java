package com.smarthome.controller;

import java.util.Optional;

import com.smarthome.model.User;
import com.smarthome.service.AuthService;
import com.smarthome.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 个人资料页：登录后展示账号信息，并支持修改保存。
 */
@Controller
public class ProfileController {

    private final AuthService authService;
    private final UserService userService;

    public ProfileController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    /**
     * 展示个人资料页。
     * @CookieValue 会从请求的 Cookie 里取出 SESSION 对应的 token。
     */
    @GetMapping("/profile")
    public String profile(@CookieValue(value = "SESSION", required = false) String token,
                          Model model) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("user", current.get());
        model.addAttribute("token", token);
        return "profile";
    }

    /** 保存修改后的个人资料。 */
    @PostMapping("/profile")
    public String saveProfile(@CookieValue(value = "SESSION", required = false) String token,
                              @RequestParam String nickname,
                              @RequestParam String email,
                              @RequestParam String bio,
                              Model model) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        userService.updateProfile(current.get().getId(), nickname, email, bio);
        model.addAttribute("user", current.get());
        model.addAttribute("saved", true);
        return "profile";
    }
}
