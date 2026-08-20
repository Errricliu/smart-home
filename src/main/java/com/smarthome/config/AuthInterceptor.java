package com.smarthome.config;

import java.util.Optional;

import com.smarthome.model.User;
import com.smarthome.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 —— 统一鉴权，收拢散落在各 Controller 里的重复代码。
 *
 * <p>这就是“AOP（面向切面编程）”在 Spring MVC 里的落地：
 * 登录校验是横切在几乎所有接口里的“通用逻辑”，不该在每个方法里各写一遍。
 * 用一个拦截器在请求进入 Controller 之前统一处理，Controller 只管业务。
 *
 * <p>流程：
 * 1. 从请求头 Authorization: Bearer <token> 取出 token；
 * 2. 调 AuthService.currentUser(token) 校验是否登录、是否过期；
 * 3. 通过 -> 把当前用户塞进 request attribute，放行；
 * 4. 不通过 -> 直接返回 401 JSON，不再进入 Controller。
 *
 * <p>白名单：/api/login、/api/register 这两个接口无需登录即可访问，
 * 在 WebConfig 里配置拦截路径时排除它们。
 *
 * <p>为什么用 HTTP Header 传 token 而不是 URL 参数：
 * URL 参数会留在浏览器历史、访问日志、反向代理日志里，容易泄露；
 * Header 更规范（业界标准），也更安全。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 存放“当前登录用户”的 request attribute key。 */
    public static final String CURRENT_USER_ATTR = "currentUser";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // CORS 预检请求（OPTIONS）直接放行，不参与鉴权
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        Optional<User> current = authService.currentUser(token);
        if (current.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=utf-8");
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }

        // 把用户放进 request，Controller 通过 @RequestAttribute 直接拿到
        request.setAttribute(CURRENT_USER_ATTR, current.get());
        return true;
    }

    /** 从 Authorization: Bearer <token> 头里取 token；缺失返回 null。 */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) {
            return null;
        }
        if (header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
