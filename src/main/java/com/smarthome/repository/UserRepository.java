package com.smarthome.repository;

import java.util.Optional;

import com.smarthome.model.User;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 用户数据访问层。
 *
 * <p>这是理解 Spring IoC 很关键的一处：我们只声明了一个“接口”，
 * 一个实现类都没写。那运行时到底是谁提供了 save / findByUsername 这些方法？
 *
 * <p>答案是：Spring Data JPA 在启动时会用“动态代理”生成这个接口的实现，
 * 并把它注册进容器。当别处需要 UserRepository 时，容器注入的就是那个代理对象。
 * 所以虽然你看到的是一行接口声明，背后其实已经发生了“扫描 -> 生成实现 -> 注入”。
 *
 * <p>findByUsername 这类方法名本身就是一个“查询约定”：Spring 会根据方法名
 * 自动拼出 SQL（相当于 SELECT ... WHERE username = ?）。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
