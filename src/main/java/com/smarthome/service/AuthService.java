package com.smarthome.service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.smarthome.exception.LoginLockedException;
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
 *
 * <p>“防暴力破解”：把原来 Node 后端里的逻辑搬了过来 ——
 * 连续 5 次输错密码，锁定 5 分钟。同样用进程内 Map 记录失败次数。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /** 会话超时时间：30 分钟无操作即失效（滑动过期）。 */
    public static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L;

    // token -> Session（含用户名 + 过期时间）。ConcurrentHashMap 保证并发安全。
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** 连续失败多少次触发锁定。 */
    private static final int MAX_WRONG_ATTEMPTS = 5;
    /** 锁定时长（毫秒）：5 分钟。 */
    private static final long LOCK_DURATION_MS = 5 * 60 * 1000L;
    // username -> [连续失败次数, 锁定截止时间戳(毫秒)]。并发安全。
    private final Map<String, long[]> loginFails = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** 会话对象：记录 token 对应的用户名和过期时间戳。 */
    private static class Session {
        final String username;
        long expiresAt;

        Session(String username, long expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }

    /**
     * 校验账号密码，成功后返回一个登录令牌（token），失败返回 empty。
     *
     * @throws LoginLockedException 该账号正处在锁定期内
     */
    public Optional<String> login(String username, String rawPassword) {
        long now = System.currentTimeMillis();
        long[] fail = loginFails.get(username);

        // 1. 先查锁定：还在锁定期内直接拒绝，告诉用户还有多久
        if (fail != null && fail[1] > now) {
            long remainSec = (fail[1] - now) / 1000;
            throw new LoginLockedException("登录错误次数过多，请 " + remainSec + " 秒后重试");
        }

        // 2. 比对密码。账号不存在也照常走一遍 matches（用空串比对），
        //    让“账号不存在”和“密码错误”的耗时一致，防止通过时间差枚举账号。
        Optional<User> found = userRepository.findByUsername(username);
        boolean matched = found.isPresent()
                && passwordEncoder.matches(rawPassword, found.get().getPassword());

        // 3. 密码不对：记一次失败，累计到上限就锁定
        if (!matched) {
            if (fail == null) {
                fail = new long[]{0, 0};
            }
            fail[0]++;
            if (fail[0] >= MAX_WRONG_ATTEMPTS) {
                fail[1] = now + LOCK_DURATION_MS;
                fail[0] = 0; // 锁定期满后重新计数
            }
            loginFails.put(username, fail);
            return Optional.empty();
        }

        // 4. 登录成功：清除失败记录，发 token（带 30 分钟过期时间）
        loginFails.remove(username);
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(username, System.currentTimeMillis() + SESSION_TIMEOUT_MS));
        return Optional.of(token);
    }

    /** 登出：把 token 从缓存里移除即可。 */
    public void logout(String token) {
        sessions.remove(token);
    }

    /**
     * 根据 token 找到当前登录的用户；找不到说明未登录或已过期。
     *
     * <p>采用“滑动过期”：每次访问都刷新过期时间，只要 30 分钟内有过操作
     * 就不会掉线，长时间不操作才会要求重新登录。
     */
    public Optional<User> currentUser(String token) {
        // token 为 null（例如浏览器没带 Cookie）时直接判定为未登录，
        // 因为 ConcurrentHashMap 不允许 null 作为 key。
        if (token == null) {
            return Optional.empty();
        }
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        // 已过期：移除并返回未登录
        if (session.expiresAt < System.currentTimeMillis()) {
            sessions.remove(token);
            return Optional.empty();
        }
        // 滑动续期：刷新过期时间
        session.expiresAt = System.currentTimeMillis() + SESSION_TIMEOUT_MS;
        return userRepository.findByUsername(session.username);
    }

    /** 当前已登录用户数（用于演示缓存里实际存了多少条）。 */
    public int activeSessionCount() {
        return sessions.size();
    }
}
