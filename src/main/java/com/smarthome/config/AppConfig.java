package com.smarthome.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 手工声明 bean 的“配置类”。
 *
 * <p>这就是 DI 的第二种方式：不用在类上贴 @Component 让 Spring 扫描，
 * 而是用 @Configuration + @Bean 明确地“告诉”容器怎么造一个对象。
 *
 * <p>PasswordEncoder 来自第三方库（spring-security-crypto），
 * 它自己并没有 @Component 注解，所以我们无法靠扫描拿到它，
 * 只能在这里 new 一个 BCryptPasswordEncoder，然后交给容器管理。
 * 之后 AuthService / UserService 需要 PasswordEncoder 时，
 * 容器就会把这个由 @Bean 方法创建的对象注入进去。
 */
@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
