package com.smarthome.controller;

import java.time.LocalDate;
import java.util.Optional;

import com.smarthome.dto.ProfileForm;
import com.smarthome.model.User;
import com.smarthome.service.AuthService;
import com.smarthome.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

/**
 * 个人资料页：登录后展示账号信息，支持修改保存。
 *
 * <p>这个类能完整演示“注解如何参与 MVC 请求处理”：
 * - DispatcherServlet 收到 POST /profile 后，把表单参数绑定到 ProfileForm
 *   （@ModelAttribute，自动做类型转换，如 "MALE" -> Gender.MALE）；
 * - @Valid 触发 ProfileForm 上所有校验注解，结果写进 BindingResult；
 * - 我们检查 hasErrors() 决定“留在本页报错”还是“交给 Service 保存”。
 */
@Controller
public class ProfileController {

    private final AuthService authService;
    private final UserService userService;

    public ProfileController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping("/profile")
    public String profile(@CookieValue(value = "SESSION", required = false) String token,
                          Model model) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        model.addAttribute("user", current.get());
        // 表单回显：把库里已有资料装进表单对象
        model.addAttribute("form", ProfileForm.from(current.get()));
        // 生日输入框的 max 属性用，保证日历不能选到未来
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("token", token);
        return "profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@CookieValue(value = "SESSION", required = false) String token,
                              @Valid @ModelAttribute("form") ProfileForm form,
                              BindingResult bindingResult,
                              Model model) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            // 校验没过：留在本页。Spring 会保留用户已填的内容并带回错误信息。
            model.addAttribute("user", current.get());
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("token", token);
            return "profile";
        }
        User updated = userService.updateProfile(current.get().getId(), form);
        model.addAttribute("user", updated);
        model.addAttribute("form", ProfileForm.from(updated));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("token", token);
        model.addAttribute("saved", true);
        return "profile";
    }
}
