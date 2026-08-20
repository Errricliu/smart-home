package com.smarthome.service;

import java.util.Optional;

import com.smarthome.dto.ProfileForm;
import com.smarthome.model.User;
import com.smarthome.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 个人资料相关的业务逻辑：注册（只建账号）、查看、修改资料。
 *
 * <p>注意它和 AuthService 都依赖 UserRepository —— 容器里只会有一个
 * UserRepository 实例（默认单例），两个 Service 注入的是同一个对象。
 *
 * <p>校验在哪里做？Controller 层用 @Valid 检查格式（手机号正则、生日不超今天），
 * Service 层只负责“合法数据怎么存”。分层清晰的校验就是正常网页服务的做法。
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
     * 注册：只创建账号 + 哈希后的密码，资料留空，之后在个人页补全。
     */
    public User register(String username, String rawPassword) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        User user = new User(username, passwordEncoder.encode(rawPassword));
        return userRepository.save(user);
    }

    /**
     * 注册（带手机号）：Node 前端注册时手机号必填，一并落库。
     * 复用上面的逻辑：先校验账号唯一，再补手机号查重。
     */
    public User register(String username, String rawPassword, String phone) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("该手机号已绑定其他账号");
        }
        User user = new User(username, passwordEncoder.encode(rawPassword));
        user.setPhone(phone);
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /** 更新头像文件名。 */
    @Transactional
    public void updateAvatar(Long id, String filename) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        user.setAvatarPath(filename);
    }

    /**
     * 注销账号：物理删除用户记录。
     * 头像文件的删除由 Controller 协调（FileStorageService），
     * Service 层只管数据本身。
     */
    @Transactional
    public void deleteAccount(Long id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("用户不存在: " + id);
        }
        userRepository.deleteById(id);
    }

    /**
     * 用表单数据更新个人资料。
     * @Transactional：这一组写操作要么全部成功、要么全部回滚。
     * JPA 的“脏检查”会在事务提交时自动把修改同步回数据库。
     */
    @Transactional
    public User updateProfile(Long id, ProfileForm form) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在: " + id));
        user.setNickname(form.getNickname());
        user.setRealName(form.getRealName());
        user.setGender(form.getGender());
        user.setBirthday(form.getBirthday());
        user.setPhone(form.getPhone());
        user.setEmail(form.getEmail());
        user.setBio(form.getBio());
        return user;
    }
}
