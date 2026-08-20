package com.smarthome.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理。
 *
 * <p>@RestControllerAdvice 是 AOP（面向切面）思想在 MVC 里的体现：
 * 不用在每个 Controller 里写 try-catch，而是声明一个“切面”，
 * 当任何 Controller 的请求处理抛出指定异常时，统一由这里接管。
 *
 * <p>这里处理的是：上传文件超过 application.properties 里配置的 2MB 上限时，
 * Spring 在解析 multipart 请求时就会抛异常（还没进 Controller 方法），
 * 不接管的话用户会看到 500 错误页。
 *
 * <p>注意：现在系统主要走 JSON API（Node 前端），所以这里返回 JSON 而不是
 * 重定向到页面，保持和 ApiController 统一的 {success, message, data} 格式。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge() {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("success", false);
        resp.put("message", "图片大小不能超过 2MB");
        resp.put("data", null);
        return ResponseEntity.ok(resp);
    }
}
