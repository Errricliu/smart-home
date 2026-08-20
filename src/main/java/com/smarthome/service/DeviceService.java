package com.smarthome.service;

import java.util.List;

import com.smarthome.model.Device;
import com.smarthome.repository.DeviceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备相关的业务逻辑。
 *
 * <p>核心权限规则（智能家居系统的关键设计）：
 * - 普通用户只能增删改查“自己名下”的设备（按 ownerId 过滤）；
 * - 管理员可以查看所有设备，但这里暂不开放管理员替用户增删设备（保持简单）。
 *
 * <p>权限校验不在 Service 做判断，而是把“谁能看到什么”的决定权交给
 * Controller（它知道当前登录用户和角色）。Service 只管数据的合法读写。
 */
@Service
public class DeviceService {

    private final DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /** 查询某个用户名下的所有设备。 */
    public List<Device> listByOwner(Long ownerId) {
        return deviceRepository.findByOwnerId(ownerId);
    }

    /** 查询所有设备（管理员用）。 */
    public List<Device> listAll() {
        return deviceRepository.findAll();
    }

    /** 给某个用户添加一台设备。 */
    public Device addDevice(Long ownerId, String name, Device.DeviceType type) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("设备名不能为空");
        }
        Device device = new Device(name, type, ownerId);
        return deviceRepository.save(device);
    }

    /** 删除设备（只允许删自己的）。 */
    @Transactional
    public void deleteDevice(Long deviceId, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (!device.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("无权操作该设备");
        }
        deviceRepository.delete(device);
    }

    /** 切换设备开关状态（只允许操作自己的）。 */
    @Transactional
    public Device toggleDevice(Long deviceId, Long ownerId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("设备不存在"));
        if (!device.getOwnerId().equals(ownerId)) {
            throw new IllegalArgumentException("无权操作该设备");
        }
        device.setStatus(device.getStatus() == Device.DeviceStatus.ON
                ? Device.DeviceStatus.OFF
                : Device.DeviceStatus.ON);
        return device;
    }
}
