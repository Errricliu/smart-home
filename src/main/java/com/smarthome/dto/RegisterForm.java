package com.smarthome.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册表单 DTO。
 *
 * <p>和 ProfileForm 同一个套路：表单字段 + 校验注解。
 * 注意“确认密码”这种两次输入是否一致的判断，没有现成的单个注解能做
 * （它涉及两个字段的关系），所以在 Controller 里手动比对，
 * 通过 bindingResult.rejectValue 把错误挂到 confirmPassword 字段上。
 */
public class RegisterForm {

    /** 账号：4-20 位字母 / 数字 / 下划线。 */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "账号需为 4-20 位字母、数字或下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需为 6-32 位")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
