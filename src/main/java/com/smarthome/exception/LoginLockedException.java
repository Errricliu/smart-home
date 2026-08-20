package com.smarthome.exception;

/**
 * 登录被锁定（连续失败次数过多）时抛出。
 *
 * <p>这是从原 Node 后端迁移过来的防暴力破解功能：
 * 连续输错 N 次后锁定一段时间，期间禁止再次尝试。
 * 抛异常而不是返回 empty，是为了能把“锁定原因”带给调用方展示给用户，
 * 而不是笼统地显示“账号或密码错误”。
 */
public class LoginLockedException extends RuntimeException {

    public LoginLockedException(String message) {
        super(message);
    }
}
