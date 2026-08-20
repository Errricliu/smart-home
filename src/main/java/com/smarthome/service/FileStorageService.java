package com.smarthome.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务：负责头像图片的保存与删除。
 *
 * <p>把文件读写从 Controller 里抽出来单独成一个 @Service：
 * Controller 只管“请求进来、响应出去”，文件怎么校验、存到哪、叫什么名字，
 * 都是这一层的职责。它也被注册进容器，由构造器注入给需要它的地方。
 *
 * <p>存储位置：项目根目录下的 uploads/avatars/（已加入 .gitignore）。
 * 真实项目会存到对象存储（OSS/S3），但“校验 -> 重命名 -> 落盘 -> 记录文件名”
 * 这套流程是一样的。
 */
@Service
public class FileStorageService {

    private static final long MAX_SIZE = 2L * 1024 * 1024; // 2MB

    /** 允许的图片类型 -> 文件扩展名。 */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private final Path avatarDir = Paths.get("uploads", "avatars");

    public FileStorageService() {
        try {
            Files.createDirectories(avatarDir);
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建头像目录", e);
        }
    }

    /**
     * 保存头像，返回生成的文件名（调用方负责把它存进用户记录）。
     * 文件名用 用户id + 随机串，避免不同用户互相覆盖。
     */
    public String saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String ext = ALLOWED_TYPES.get(file.getContentType());
        if (ext == null) {
            throw new IllegalArgumentException("只支持 JPG / PNG / WebP / GIF 图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("图片大小不能超过 2MB");
        }
        String filename = userId + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
        try {
            file.transferTo(avatarDir.resolve(filename));
        } catch (IOException e) {
            throw new UncheckedIOException("头像保存失败", e);
        }
        return filename;
    }

    /** 删除头像文件（换头像、注销账号时调用）。删不掉不影响主流程。 */
    public void deleteAvatar(String filename) {
        if (filename == null) {
            return;
        }
        try {
            Files.deleteIfExists(avatarDir.resolve(filename));
        } catch (IOException ignored) {
            // 文件残留无害，忽略
        }
    }
}
