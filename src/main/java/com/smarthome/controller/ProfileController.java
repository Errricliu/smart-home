package com.smarthome.controller;

import java.time.LocalDate;
import java.util.Optional;

import com.smarthome.dto.ProfileForm;
import com.smarthome.model.User;
import com.smarthome.service.AuthService;
import com.smarthome.service.FileStorageService;
import com.smarthome.service.UserService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * 个人资料页：展示 / 修改资料、上传头像、注销账号。
 *
 * <p>这个类现在有 3 个依赖（AuthService / UserService / FileStorageService），
 * 全部由容器通过构造器注入。可以在启动日志里观察到：
 * 容器会先创建这三个 Service（以及它们依赖的 UserRepository / PasswordEncoder），
 * 最后才创建这个 Controller——依赖链的末端。
 */
@Controller
public class ProfileController {

    private final AuthService authService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public ProfileController(AuthService authService,
                             UserService userService,
                             FileStorageService fileStorageService) {
        this.authService = authService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/profile")
    public String profile(@CookieValue(value = "SESSION", required = false) String token,
                          Model model) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        fillModel(model, current.get(), token, false);
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
            // 注意：这里不能覆盖 model 里的 "form"！
            // 用户提交的表单对象和它的 BindingResult（错误信息）是配对的，
            // 一旦替换 form，Spring 会把对应的错误信息也从模型里移除，
            // 页面就渲染不出错误提示了。所以这里只补充其他展示数据。
            model.addAttribute("user", current.get());
            model.addAttribute("today", LocalDate.now());
            model.addAttribute("token", token);
            return "profile";
        }
        User updated = userService.updateProfile(current.get().getId(), form);
        fillModel(model, updated, token, true);
        return "profile";
    }

    /**
     * 上传头像。MultipartFile 是 Spring 对 multipart/form-data
     * 文件上传的封装，DispatcherServlet 会自动把文件部分绑定到这个参数。
     */
    @PostMapping("/profile/avatar")
    public String uploadAvatar(@CookieValue(value = "SESSION", required = false) String token,
                               @RequestParam("file") MultipartFile file,
                               RedirectAttributes redirectAttributes) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        try {
            String filename = fileStorageService.saveAvatar(current.get().getId(), file);
            fileStorageService.deleteAvatar(current.get().getAvatarPath()); // 删掉旧头像
            userService.updateAvatar(current.get().getId(), filename);
        } catch (IllegalArgumentException e) {
            // addFlashAttribute：错误信息只存活一次重定向，到个人页展示
            redirectAttributes.addFlashAttribute("avatarError", e.getMessage());
        }
        return "redirect:/profile";
    }

    /** 注销账号：删头像文件 -> 删用户记录 -> 清会话 -> 回登录页。 */
    @PostMapping("/account/delete")
    public String deleteAccount(@CookieValue(value = "SESSION", required = false) String token,
                                HttpServletResponse response) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return "redirect:/login";
        }
        fileStorageService.deleteAvatar(current.get().getAvatarPath());
        userService.deleteAccount(current.get().getId());
        authService.logout(token);

        Cookie cookie = new Cookie("SESSION", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        return "redirect:/login";
    }

    /** 把渲染个人页需要的公共数据装进 Model，避免三个方法重复写。 */
    private void fillModel(Model model, User user, String token, boolean saved) {
        model.addAttribute("user", user);
        model.addAttribute("form", ProfileForm.from(user));
        model.addAttribute("today", LocalDate.now());
        model.addAttribute("token", token);
        model.addAttribute("saved", saved);
    }
}
