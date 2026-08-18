package com.smarthome.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 用户实体。
 *
 * <p>这里用一个普通 Java 类（POJO）+ 几个注解，就把它变成了一张数据库表。
 * JPA 会读取这些注解，自动帮我们在数据库里建表、把对象的字段映射成列。
 *
 * <p>注意：这个类本身只是一个“数据容器”，它并不知道 Spring 的存在。
 * 之后它会被当作参数在 Controller / Service / Repository 之间传递。
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 登录账号，必须唯一。 */
    @Column(nullable = false, unique = true)
    private String username;

    /** 密码的哈希值，绝不直接存明文。 */
    @Column(nullable = false)
    private String password;

    /** 昵称，用于在个人页展示。 */
    private String nickname;

    /** 邮箱，可修改。 */
    private String email;

    /** 一句话自我介绍，可修改。 */
    private String bio;

    // JPA 要求有一个无参构造方法，方便它用反射创建对象。
    protected User() {
    }

    public User(String username, String password, String nickname, String email, String bio) {
        this.username = username;
        this.password = password;
        this.nickname = nickname;
        this.email = email;
        this.bio = bio;
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
