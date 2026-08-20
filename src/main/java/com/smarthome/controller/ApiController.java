package com.smarthome.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import com.smarthome.config.AuthInterceptor;
import com.smarthome.dto.ProfileForm;
import com.smarthome.exception.LoginLockedException;
import com.smarthome.model.Gender;
import com.smarthome.model.Role;
import com.smarthome.model.User;
import com.smarthome.service.AuthService;
import com.smarthome.service.FileStorageService;
import com.smarthome.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * JSON API —— 给 Node.js 前端调用的“跨语言”接口。
 *
 * <p>架构：浏览器 -> main.html(JS) -> Node(代理) -> 本控制器 -> MySQL
 * 这里的每个接口都返回统一的 JSON 结构 {success, message, data}，
 * 和原来 Node 后端返回的格式保持一致，这样 main.html 里的 JS 只需要
 * 改字段名、不用改整体判断逻辑（顺带把原来 data.code === 1 的判断修对）。
 *
 * <p>为什么需要这个控制器：之前那些 AuthController / ProfileController 返回的是
 * Thymeleaf 页面（HTML），而 Node 前端要的是纯数据（JSON）。
 * 同一个业务逻辑，两种“出口”：页面出口和 JSON 出口，这就是本类的职责。
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final AuthService authService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    /** 与 RegisterForm 保持一致的校验规则。 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{4,20}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public ApiController(AuthService authService, UserService userService,
                         FileStorageService fileStorageService) {
        this.authService = authService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    // ---------- 登录 ----------

    /** POST /api/login  body: {username, password} */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return ok(false, "账号和密码不能为空", null);
        }
        Optional<String> token;
        try {
            token = authService.login(username.trim(), password);
        } catch (LoginLockedException e) {
            return ok(false, e.getMessage(), null);
        }
        if (token.isEmpty()) {
            return ok(false, "账号或密码错误", null);
        }
        // 登录成功：把 token 和 userId 一起给前端
        // userId 用于页面上补零展示（如 000001），token 用于后续请求的身份凭证
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token.get());
        data.put("username", username.trim());
        authService.currentUser(token.get()).ifPresent(u -> {
            data.put("userId", u.getId());
            data.put("role", u.getRole().name());
        });
        return ok(true, "登录成功", data);
    }

    // ---------- 注册 ----------

    /** POST /api/register  body: {username, password, phone} */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String phone = body.get("phone");

        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            return ok(false, "账号需为 4-20 位字母、数字或下划线", null);
        }
        if (password == null || password.length() < 6 || password.length() > 32) {
            return ok(false, "密码长度需为 6-32 位", null);
        }
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            return ok(false, "手机号格式不正确", null);
        }
        try {
            userService.register(username, password, phone);
            return ok(true, "注册成功，请登录", null);
        } catch (IllegalArgumentException e) {
            return ok(false, e.getMessage(), null);
        }
    }

    // ---------- 查询 / 修改个人资料 ----------

    /**
     * GET /api/user —— 返回当前登录用户的完整资料。
     * 用户已由 AuthInterceptor 鉴权并注入，这里直接拿。
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUser(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTR) User user) {
        return ok(true, "获取成功", toUserData(user));
    }

    /**
     * POST /api/user  body: {realName, gender, birthday, phone, email, bio} —— 修改资料。
     * token 不再放 body 里，改由拦截器从 Header 鉴权注入。
     */
    @PostMapping("/user")
    public ResponseEntity<Map<String, Object>> updateUser(
            @RequestBody Map<String, String> body,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTR) User user) {

        // 字段校验（与 ProfileForm 规则对齐；nickname 允许缺省，沿用原值）
        String realName = body.get("realName");
        if (realName == null || realName.isBlank()) {
            return ok(false, "真实姓名不能为空", null);
        }
        // 手机号视为不可修改：前端不提交时沿用原值，提交了则校验格式
        String phone = body.get("phone");
        if (phone == null || phone.isBlank()) {
            phone = user.getPhone();
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            return ok(false, "手机号格式不正确", null);
        }
        String email = body.get("email");
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return ok(false, "邮箱格式不正确", null);
        }
        LocalDate birthday;
        try {
            birthday = LocalDate.parse(body.get("birthday"));
        } catch (DateTimeParseException | NullPointerException e) {
            return ok(false, "生日格式不正确", null);
        }
        if (birthday.isAfter(LocalDate.now())) {
            return ok(false, "生日不能晚于今天", null);
        }
        Gender gender;
        try {
            gender = Gender.valueOf(body.getOrDefault("gender", "SECRET"));
        } catch (IllegalArgumentException e) {
            return ok(false, "性别取值不正确", null);
        }

        // 组装成 ProfileForm 交给 UserService（业务逻辑复用，不留两套）
        ProfileForm form = new ProfileForm();
        form.setNickname(body.getOrDefault("nickname", user.getNickname()));
        form.setRealName(realName);
        form.setGender(gender);
        form.setBirthday(birthday);
        form.setPhone(phone);
        form.setEmail(email);
        form.setBio(body.get("bio"));

        try {
            User updated = userService.updateProfile(user.getId(), form);
            return ok(true, "资料修改成功", toUserData(updated));
        } catch (IllegalArgumentException e) {
            // 例如“该姓名已被使用”，属于业务校验失败，返回给前端展示
            return ok(false, e.getMessage(), null);
        }
    }

    // ---------- 管理员接口 ----------

    /**
     * GET /api/admin/users?token=xxx —— 管理员查看所有用户（普通用户调用会被拒绝）。
     *
     * <p>隐私保护：管理员看“别人”的手机号/邮箱/姓名时做脱敏，
     * 只有看“自己”时才返回完整信息。这是真实系统里保护用户隐私的标准做法——
     * 即便是管理员，也不该能看到用户的明文隐私数据。
     */
    @GetMapping("/admin/users")
    public ResponseEntity<Map<String, Object>> listAllUsers(
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTR) User current) {
        if (current.getRole() != Role.ADMIN) {
            return ok(false, "无权限：仅管理员可查看", null);
        }
        Long selfId = current.getId();
        List<Map<String, Object>> users = new ArrayList<>();
        for (User u : userService.listAll()) {
            Map<String, Object> ud = toUserData(u);
            // 看别人时脱敏，看自己时保留完整
            if (!u.getId().equals(selfId)) {
                ud.put("phone", maskPhone(u.getPhone()));
                ud.put("email", maskEmail(u.getEmail()));
                ud.put("realName", maskName(u.getRealName()));
            }
            users.add(ud);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("users", users);
        return ok(true, "获取成功", data);
    }

    /** 手机号脱敏：138****8000 */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /** 邮箱脱敏：l***@example.com（保留首字符和域名） */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String head = local.length() > 1 ? local.substring(0, 1) : local;
        return head + "***" + domain;
    }

    /** 姓名脱敏：张三 -> 张*；单个字则保留原样 */
    private String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name;
        return name.substring(0, 1) + "*".repeat(name.length() - 1);
    }

    // ---------- 头像上传 ----------

    /**
     * POST /api/user/avatar  multipart/form-data: {file}
     * 上传头像：校验图片类型和大小 -> 落盘 -> 记录文件名 -> 返回头像 URL。
     * 用户由拦截器从 Header 鉴权注入。
     */
    @PostMapping("/user/avatar")
    public ResponseEntity<Map<String, Object>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestAttribute(AuthInterceptor.CURRENT_USER_ATTR) User user) {
        try {
            String filename = fileStorageService.saveAvatar(user.getId(), file);
            // 换头像：先删旧文件，再更新记录
            fileStorageService.deleteAvatar(user.getAvatarPath());
            userService.updateAvatar(user.getId(), filename);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("avatarUrl", "/avatars/" + filename);
            return ok(true, "头像上传成功", data);
        } catch (IllegalArgumentException e) {
            return ok(false, e.getMessage(), null);
        }
    }

    // ---------- 工具方法 ----------

    /** 把 User 实体转成给前端的安全数据（绝不含 passwordHash）。 */
    private Map<String, Object> toUserData(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("realName", user.getRealName());
        data.put("gender", user.getGender() == null ? null : user.getGender().name());
        data.put("genderLabel", user.getGender() == null ? null : user.getGender().getLabel());
        data.put("birthday", user.getBirthday() == null ? null : user.getBirthday().toString());
        data.put("phone", user.getPhone());
        data.put("email", user.getEmail());
        data.put("bio", user.getBio());
        data.put("role", user.getRole() == null ? "USER" : user.getRole().name());
        // 头像 URL：null 表示还没上传，前端显示默认占位
        data.put("avatarUrl", user.getAvatarPath() == null ? null : "/avatars/" + user.getAvatarPath());
        return data;
    }

    /** 统一响应：和原来 Node 后端一致的 {success, message, data} 结构。 */
    private ResponseEntity<Map<String, Object>> ok(boolean success, String message, Map<String, Object> data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("message", message);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}
