package com.smarthome.model;

/**
 * 性别枚举。
 *
 * <p>数据库里存的是字符串（配合 User 实体上的 @Enumerated(EnumType.STRING)），
 * 页面上展示的是中文 label。用枚举而不是裸字符串，可以避免存进库里
 * 出现“男 / male / M”这种不一致的脏数据。
 */
public enum Gender {

    MALE("男"),
    FEMALE("女"),
    SECRET("保密");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
