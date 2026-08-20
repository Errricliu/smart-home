package com.smarthome.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户实体。
 *
 * <p>一个普通 Java 类（POJO）+ 几个注解，就对应数据库里的一张 users 表。
 * JPA 读取这些注解完成“对象 <-> 表”的映射。
 *
 * <p>字段分两类：
 * - 程序管理的：id（程序赋予，不可从网页修改）、username、password（哈希）；
 * - 用户可改的资料：nickname、realName、gender、birthday、phone、email、bio，
 *   它们通过 ProfileForm 表单提交、由 UserService.updateProfile 写回。
 */
@Entity
@Table(name = "users")
public class User {

    /** 用户id，数据库自增，程序赋予。网页上只展示，不提供修改入口。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录账号，唯一，注册后不可改。 */
    @Column(nullable = false, unique = true)
    private String username;

    /** 密码的哈希值，绝不存明文。 */
    @Column(nullable = false)
    private String password;

    /** 昵称（展示用，可改）。 */
    private String nickname;

    /** 真实姓名（可改，全平台唯一，不允许重名）。 */
    @Column(name = "real_name", unique = true)
    private String realName;

    /** 角色：普通用户 / 管理员。默认普通用户。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    /** 性别。@Enumerated(STRING) 让库里存 "MALE"/"FEMALE" 而不是序号。 */
    @Enumerated(EnumType.STRING)
    private Gender gender;

    /** 生日（可改，网页上限制不能超过今天）。 */
    private LocalDate birthday;

    /** 手机号（可改，有格式校验）。 */
    private String phone;

    /** 邮箱（可改，有格式校验）。 */
    private String email;

    /** 一句话简介（选填）。 */
    private String bio;

    /** 头像文件名（存 uploads/avatars/ 下），null 表示未上传。 */
    @Column(name = "avatar_path")
    private String avatarPath;

    // JPA 要求无参构造，便于它用反射创建对象。
    protected User() {
    }

    /** 注册时只需要账号和密码，资料可以之后在个人页补全。 */
    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }
}
