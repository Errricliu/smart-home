package com.smarthome.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.smarthome.model.User;
import com.smarthome.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 登录 / 登出 业务逻辑。
 *
 * <p>先看这个类的构造方法：它需要 UserRepository 和 PasswordEncoder 两个参数，
 * 但我们从没 new 过它。Spring 在启动扫描到这个 @Service 时，会先去找
 * UserRepository 和 PasswordEncoder 这两个“依赖”，把它们准备好后，
 * 再通过构造方法传进来 —— 这就是你理解的“一层一层、先创建没有依赖的 bean”。
 *
 * <p>“登录状态缓存”这里用一个进程内的 Map（token -> username）来保存。
 * 真实项目会用 Redis 或 Spring Session 存到外部，这里先用最简单的方式，
 * 让你看清“登录后如何保持状态”这件事本身。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // token -> username。ConcurrentHashMap 保证并发安全。
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 校验账号密码，成功后返回一个登录令牌（token），失败返回 empty。
     */
    public Optional<String> login(String username, String rawPassword) {
        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        User user = found.get();
        // matches：拿用户输入的明文，和库里存的哈希做比对。
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            return Optional.empty();
        }
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return Optional.of(token);
    }

    /** 登出：把 token 从缓存里移除即可。 */
    public void logout(String token) {
        sessions.remove(token);
    }

    /** 根据 token 找到当前登录的用户；找不到说明未登录或已过期。 */
    public Optional<User> currentUser(String token) {
        // token 为 null（例如浏览器没带 Cookie）时直接判定为未登录，
        // 因为 ConcurrentHashMap 不允许 null 作为 key。
        if (token == null) {
            return Optional.empty();
        }
        String username = sessions.get(token);
        if (username == null) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    /** 当前已登录用户数（用于演示缓存里实际存了多少条）。 */
    public int activeSessionCount() {
        return sessions.size();
    }
}
