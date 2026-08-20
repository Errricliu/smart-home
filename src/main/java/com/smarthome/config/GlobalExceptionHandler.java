package com.smarthome.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 全局异常处理。
 *
 * <p>@ControllerAdvice 是 AOP（面向切面）思想在 MVC 里的体现：
 * 不用在每个 Controller 里写 try-catch，而是声明一个“切面”，
 * 当任何 Controller 的请求处理抛出指定异常时，统一由这里接管。
 *
 * <p>这里处理的是：上传文件超过 application.properties 里配置的 2MB 上限时，
 * Spring 在解析 multipart 请求时就会抛异常（还没进 Controller 方法），
 * 不接管的话用户会看到 500 错误页。接管后重定向回个人页并给出提示。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleUploadTooLarge(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("avatarError", "图片大小不能超过 2MB");
        return "redirect:/profile";
    }
}
