package com.smarthome.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 智能家居设备实体。
 *
 * <p>一个家庭里有很多设备：灯、空调、电视、窗帘……每台设备属于某个用户（ownerId）。
 * 用户只能看到和操作自己的设备；管理员能看到所有设备。
 *
 * <p>字段说明：
 * - name：设备名（如“客厅灯”）
 * - type：设备类型（LIGHT/AC/TV/CURTAIN…），前端据此显示不同图标
 * - status：开关状态（ON/OFF）
 * - battery：电量（0-100，可选，有些设备没有电量如空调）
 * - ownerId：归属用户 id（关联 users.id）
 */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 设备名，不可空。 */
    @Column(nullable = false)
    private String name;

    /** 设备类型，用枚举存，避免脏数据。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType type;

    /** 开关状态。 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceStatus status = DeviceStatus.OFF;

    /** 电量 0-100，null 表示该设备无电量概念。 */
    private Integer battery;

    /** 归属用户 id。 */
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    protected Device() {
    }

    public Device(String name, DeviceType type, Long ownerId) {
        this.name = name;
        this.type = type;
        this.ownerId = ownerId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DeviceType getType() {
        return type;
    }

    public void setType(DeviceType type) {
        this.type = type;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        this.status = status;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    /** 设备类型枚举。 */
    public enum DeviceType {
        LIGHT("灯"),
        AC("空调"),
        TV("电视"),
        CURTAIN("窗帘"),
        SPEAKER("音箱"),
        SENSOR("传感器");

        private final String label;

        DeviceType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** 设备开关状态枚举。 */
    public enum DeviceStatus {
        ON("开启"),
        OFF("关闭");

        private final String label;

        DeviceStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
