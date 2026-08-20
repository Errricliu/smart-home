package com.smarthome.dto;

import java.time.LocalDate;

import com.smarthome.model.Gender;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

/**
 * 个人资料表单的 DTO（Data Transfer Object，数据传输对象）。
 *
 * <p>为什么不直接用 User 实体接表单？因为网页表单和数据库实体是两个关注点：
 * 表单里没有 userid / username / 密码哈希，而实体里这些字段不该被网页随便改。
 * 用一个专门的类承接表单，字段一一对应但各自独立——这是正常网页服务的常见做法。
 *
 * <p>这里的校验注解来自 Jakarta Bean Validation：
 * Controller 方法参数上加 @Valid，Spring MVC 在“数据绑定”阶段就会逐条检查，
 * 不通过的写进 BindingResult，由我们自己决定怎么展示错误。
 * 这正是“注解不仅标记 bean，还参与 MVC 请求处理”的体现。
 */
public class ProfileForm {

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    @NotNull(message = "请选择性别")
    private Gender gender;

    /** @DateTimeFormat 告诉 Spring 如何把 "2000-01-01" 这样的字符串转成 LocalDate。 */
    @NotNull(message = "请填写生日")
    @PastOrPresent(message = "生日不能超过今天")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthday;

    /** 中国大陆手机号：1 开头，第二位 3-9，共 11 位数字。 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 简介是选填的，不加校验注解。 */
    private String bio;

    public ProfileForm() {
    }

    public ProfileForm(String nickname, String realName, Gender gender,
                       LocalDate birthday, String phone, String email, String bio) {
        this.nickname = nickname;
        this.realName = realName;
        this.gender = gender;
        this.birthday = birthday;
        this.phone = phone;
        this.email = email;
        this.bio = bio;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
