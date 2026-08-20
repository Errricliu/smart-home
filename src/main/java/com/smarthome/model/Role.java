package com.smarthome.model;

/**
 * 用户角色枚举。
 *
 * <p>智能家居系统里通常有两种角色：
 * - USER：普通用户，只能管理自己的设备、看自己的资料；
 * - ADMIN：管理员，能查看全平台所有用户和所有设备（但不能替用户改密码）。
 *
 * <p>和 Gender 一样用枚举 + @Enumerated(STRING)，数据库里存 "USER"/"ADMIN"。
 */
public enum Role {
    USER("普通用户"),
    ADMIN("管理员");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
