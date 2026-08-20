package com.smarthome;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 个人资料页的完整链路测试（MockMvc 模拟浏览器，不起真实端口）。
 *
 * <p>MockMvc 的请求同样会经过 DispatcherServlet，所以测的就是真实的
 * “表单绑定 -> @Valid 校验 -> Controller 分支”逻辑。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProfileFlowTest {

    @Autowired
    private MockMvc mockMvc;

    /** 未登录访问个人页：应被重定向到登录页。 */
    @Test
    void profileRequiresLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    /** 非法手机号：校验失败，页面显示错误信息。 */
    @Test
    void invalidPhoneShowsError() throws Exception {
        Cookie session = loginAsAdmin();
        mockMvc.perform(post("/profile").cookie(session)
                        .param("nickname", "管理员")
                        .param("realName", "张三")
                        .param("gender", "MALE")
                        .param("birthday", "2000-01-01")
                        .param("phone", "123")          // 非法：不是 11 位手机号
                        .param("email", "admin@smarthome.com")
                        .param("bio", "简介"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("手机号格式不正确")));
    }

    /** 未来日期的生日：校验失败。 */
    @Test
    void futureBirthdayShowsError() throws Exception {
        Cookie session = loginAsAdmin();
        mockMvc.perform(post("/profile").cookie(session)
                        .param("nickname", "管理员")
                        .param("realName", "张三")
                        .param("gender", "MALE")
                        .param("birthday", "2999-01-01") // 非法：超过今天
                        .param("phone", "13800138000")
                        .param("email", "admin@smarthome.com")
                        .param("bio", "简介"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("生日不能超过今天")));
    }

    /** 合法数据：保存成功，页面显示“已保存”。 */
    @Test
    void validProfileSaves() throws Exception {
        Cookie session = loginAsAdmin();
        mockMvc.perform(post("/profile").cookie(session)
                        .param("nickname", "管理员")
                        .param("realName", "李四")
                        .param("gender", "FEMALE")
                        .param("birthday", "1995-06-15")
                        .param("phone", "13912345678")
                        .param("email", "admin@smarthome.com")
                        .param("bio", "更新后的简介"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("已保存")))
                .andExpect(content().string(containsString("李四")));
    }

    /** 用演示账号登录，取回 SESSION Cookie 供后续请求使用。 */
    private Cookie loginAsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .param("username", "admin")
                        .param("password", "123456"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        return result.getResponse().getCookie("SESSION");
    }
}
