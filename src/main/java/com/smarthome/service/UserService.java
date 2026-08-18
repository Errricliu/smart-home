package com.smarthome.service;

import java.util.Optional;

import com.smarthome.model.User;
import com.smarthome.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人资料相关的业务逻辑：注册（初始化用户）、查看、修改。
 *
 * <p>注意它和 AuthService 都依赖 UserRepository —— 容器里只会有一个
 * UserRepository 的实例（默认单例），两个 Service 拿到的是同一个对象。
 * 这正是 IoC 容器的价值：共享的依赖不用各自 new 一份。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 注册一个新用户。密码在这里被哈希后再存库。
     * encode(raw) 就是“哈希”动作，存进去的是一串乱码，不是明文。
     */
    public User register(String username, String rawPassword, String nickname, String email, String bio) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        User user = new User(
                username,
                passwordEncoder.encode(rawPassword),
                nickname,
                email,
                bio
        );
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 更新个人资料（昵称 / 邮箱 / 简介）。账号名和密码不在这里改。
     * @Transactional 表示这一组写库操作要么全部成功、要么全部回滚。
     */
    @Transactional
    public User updateProfile(Long id, String nickname, String email, String bio) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setBio(bio);
        // JPA 的“脏检查”：对受管对象做修改，事务提交时自动同步回数据库。
        return user;
    }
}
