package com.smarthome.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.smarthome.model.Device;
import com.smarthome.model.Role;
import com.smarthome.model.User;
import com.smarthome.service.AuthService;
import com.smarthome.service.DeviceService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设备 JSON API —— 智能家居的核心接口。
 *
 * <p>权限设计（智能家居系统的关键）：
 * - 普通用户：只能列出/增删/开关“自己名下”的设备；
 * - 管理员：可以列出所有设备（用于后台总览）。
 *
 * <p>每次请求都要带 token，Controller 先通过 AuthService 认出“当前是谁”，
 * 再决定允许它做什么。这就是最简单的权限控制思路：
 * 认身份（认证）→ 判权限（授权），两步分开。
 */
@RestController
@RequestMapping("/api/device")
public class DeviceController {

    private final AuthService authService;
    private final DeviceService deviceService;

    public DeviceController(AuthService authService, DeviceService deviceService) {
        this.authService = authService;
        this.deviceService = deviceService;
    }

    /** GET /api/device/list?token=xxx —— 列出设备（普通用户看自己的，管理员看全部）。 */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String token) {
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            return ok(false, "未登录或登录已过期", null);
        }
        User user = current.get();
        List<Device> devices = user.getRole() == Role.ADMIN
                ? deviceService.listAll()
                : deviceService.listByOwner(user.getId());

        List<Map<String, Object>> list = new ArrayList<>();
        for (Device d : devices) {
            list.add(toDeviceData(d));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("devices", list);
        data.put("isAdmin", user.getRole() == Role.ADMIN);
        return ok(true, "获取成功", data);
    }

    /** POST /api/device/add  body: {token, name, type} —— 添加设备（仅自己）。 */
    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> add(@RequestBody Map<String, String> body) {
        Optional<User> current = authService.currentUser(body.get("token"));
        if (current.isEmpty()) {
            return ok(false, "未登录或登录已过期", null);
        }
        String name = body.get("name");
        Device.DeviceType type;
        try {
            type = Device.DeviceType.valueOf(body.get("type"));
        } catch (IllegalArgumentException | NullPointerException e) {
            return ok(false, "设备类型不正确", null);
        }
        try {
            Device device = deviceService.addDevice(current.get().getId(), name, type);
            return ok(true, "添加成功", toDeviceData(device));
        } catch (IllegalArgumentException e) {
            return ok(false, e.getMessage(), null);
        }
    }

    /** POST /api/device/toggle  body: {token, deviceId} —— 切换开关（仅自己）。 */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggle(@RequestBody Map<String, String> body) {
        Optional<User> current = authService.currentUser(body.get("token"));
        if (current.isEmpty()) {
            return ok(false, "未登录或登录已过期", null);
        }
        Long deviceId;
        try {
            deviceId = Long.valueOf(body.get("deviceId"));
        } catch (NumberFormatException | NullPointerException e) {
            return ok(false, "设备ID不正确", null);
        }
        try {
            Device device = deviceService.toggleDevice(deviceId, current.get().getId());
            return ok(true, "操作成功", toDeviceData(device));
        } catch (IllegalArgumentException e) {
            return ok(false, e.getMessage(), null);
        }
    }

    /** POST /api/device/delete  body: {token, deviceId} —— 删除设备（仅自己）。 */
    @PostMapping("/delete")
    public ResponseEntity<Map<String, Object>> delete(@RequestBody Map<String, String> body) {
        Optional<User> current = authService.currentUser(body.get("token"));
        if (current.isEmpty()) {
            return ok(false, "未登录或登录已过期", null);
        }
        Long deviceId;
        try {
            deviceId = Long.valueOf(body.get("deviceId"));
        } catch (NumberFormatException | NullPointerException e) {
            return ok(false, "设备ID不正确", null);
        }
        try {
            deviceService.deleteDevice(deviceId, current.get().getId());
            return ok(true, "删除成功", null);
        } catch (IllegalArgumentException e) {
            return ok(false, e.getMessage(), null);
        }
    }

    // ---------- 工具 ----------

    private Map<String, Object> toDeviceData(Device d) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("deviceId", d.getId());
        data.put("name", d.getName());
        data.put("type", d.getType().name());
        data.put("typeLabel", d.getType().getLabel());
        data.put("status", d.getStatus().name());
        data.put("statusLabel", d.getStatus().getLabel());
        data.put("battery", d.getBattery());
        data.put("ownerId", d.getOwnerId());
        return data;
    }

    private ResponseEntity<Map<String, Object>> ok(boolean success, String message, Map<String, Object> data) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", success);
        resp.put("message", message);
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }
}
