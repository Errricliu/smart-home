package com.smarthome.repository;

import java.util.List;

import com.smarthome.model.Device;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 设备数据访问层。
 *
 * <p>和 UserRepository 一样，只声明接口，Spring Data JPA 动态生成实现。
 * findByOwnerId：按归属用户查设备；findAll 用于管理员查看全平台设备。
 */
public interface DeviceRepository extends JpaRepository<Device, Long> {

    List<Device> findByOwnerId(Long ownerId);
}
