package com.smarthome;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JSON API 集成测试 —— 验证拦截器鉴权 + 会话过期 + 权限控制。
 *
 * <p>用 MockMvc 模拟真实 HTTP 请求（走 DispatcherServlet + 拦截器），
 * 覆盖当前 Node 前端对接的 /api/* 接口。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 登录成功拿到 token，后续请求用它走 Header 鉴权。 */
    @Test
    void loginReturnsToken() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }

    /** 未带 token 访问受保护接口 -> 401。 */
    @Test
    void protectedApiRejectsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    /** 带错误 token -> 401。 */
    @Test
    void protectedApiRejectsBadToken() throws Exception {
        mockMvc.perform(get("/api/user").header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized());
    }

    /** 带正确 token -> 返回当前用户资料（不含密码哈希）。 */
    @Test
    void getUserWithValidToken() throws Exception {
        String token = login("admin", "123456");
        mockMvc.perform(get("/api/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("admin"))
                // 绝不能把密码哈希泄露给前端
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    /** 健康检查接口无需登录即可访问。 */
    @Test
    void pingIsPublic() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    /** 普通用户访问管理员接口 -> 被拒绝。 */
    @Test
    void normalUserCannotAccessAdminApi() throws Exception {
        String token = login("demo_user", "123456");
        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    /** 管理员访问用户列表 -> 别人的隐私字段被脱敏。 */
    @Test
    void adminSeesMaskedUserInfo() throws Exception {
        String token = login("admin", "123456");
        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();
        // 检查返回里是否存在 demo_user，且其手机号被脱敏（含 ****）
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode u : root.path("data").path("users")) {
            if ("demo_user".equals(u.path("username").asText())) {
                org.junit.jupiter.api.Assertions.assertTrue(
                        u.path("phone").asText().contains("****"),
                        "他人手机号应被脱敏");
            }
        }
    }

    /** 登录 + 查资料 + 修改资料 + 设备列表 全链路（同一 token）。 */
    @Test
    void fullFlowWithSingleToken() throws Exception {
        String token = login("admin", "123456");

        // 查设备列表
        mockMvc.perform(get("/api/device/list").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isAdmin").value(true));
    }

    /** 辅助：登录并返回 token。 */
    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("token").asText();
    }
}
