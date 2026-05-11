package com.axercode.server.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.axercode.server.bootstrap.AxerCodeServerApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        classes = AxerCodeServerApplication.class,
        properties = {
                "axercode.storage.database-file=target/test-data/step26-web-ui.db",
                "server.port=0"
        }
)
@AutoConfigureMockMvc
class AxerCodeWebUiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootServesMinimalChatPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));

        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("AxerCode")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"appShell\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sidebar\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"chatViewport\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"topbar-utility topbar-utility-left\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"runtime-toolbar topbar-utility topbar-utility-right\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sessionList\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"newChatButton\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sidebarToggleButton\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"composer-plan-mode\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/assets/styles.css")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/assets/app.js")));
    }

    @Test
    void stylesAssetIsServed() throws Exception {
        mockMvc.perform(get("/assets/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".sidebar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".topbar-utility")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".session-list")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".composer-submit")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".composer-plan-mode")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".app-shell.sidebar-collapsed .sidebar")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".message.user .message-card")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("width: fit-content;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".message.user .message-role")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("display: none;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("min-height: calc(1em * 1.5 + 0.1rem);")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("overflow-y: hidden;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("overflow: hidden;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--sidebar-bg: #f3f4f6;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("--accent: #a78bfa;")));
    }

    @Test
    void appScriptIsServed() throws Exception {
        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fetch('/api/chat/stream'")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fetch('/api/sessions?limit=20')")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("axercode.sidebarCollapsed")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("assistantLabel(")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("appendMessage('user', '', prompt);")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("const COMPOSER_MAX_ROWS = 10;")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("resizeComposerInput()")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("updateSidebarToggleLabel()")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("sessionList")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fetch(`/api/sessions/${sessionId}`)")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("renderSessionList")));
    }
}
