package com.smarthome.controller;

import com.smarthome.dto.RegisterForm;
import com.smarthome.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import jakarta.validation.Valid;

/**
 * 注册新账号。
 *
 * <p>注意这个 Controller 只有 UserService 一个依赖——
 * 注册成功后不自动登录，而是跳回登录页让用户自己登录，
 * 这是正常网页服务的常见流程。
 */
@Controller
public class RegisterController {

    private final UserService userService;

    public RegisterController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("form") RegisterForm form,
                             BindingResult bindingResult) {
        // “两次密码一致”涉及两个字段的关系，没有现成的字段级注解，手动校验
        if (!bindingResult.hasErrors()
                && !form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "两次输入的密码不一致");
        }
        // 账号是否已存在要查库才知道，由 Service 抛异常，这里转成表单错误
        if (!bindingResult.hasErrors()) {
            try {
                userService.register(form.getUsername(), form.getPassword());
            } catch (IllegalArgumentException e) {
                bindingResult.rejectValue("username", "duplicate", e.getMessage());
            }
        }
        if (bindingResult.hasErrors()) {
            return "register";
        }
        // ?registered 让登录页显示“注册成功，请登录”
        return "redirect:/login?registered";
    }
}
