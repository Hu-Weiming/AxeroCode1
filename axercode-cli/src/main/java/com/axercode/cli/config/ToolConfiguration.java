package com.axercode.cli.config;

import com.axercode.agent.ConversationAgent;
import com.axercode.agent.ToolCallingAgent;
import com.axercode.core.session.SessionContextWindow;
import com.axercode.provider.api.LlmProvider;
import com.axercode.tools.AxerTool;
import com.axercode.tools.builtin.CreateDirectoryTool;
import com.axercode.tools.builtin.ListDirectoryTool;
import com.axercode.tools.builtin.ReadFileTool;
import com.axercode.tools.builtin.RunShellTool;
import com.axercode.tools.builtin.WriteFileTool;
import com.axercode.tools.execution.ToolExecutor;
import com.axercode.tools.registry.ToolRegistry;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfiguration {

    @Bean
    public ReadFileTool readFileTool() {
        return new ReadFileTool();
    }

    @Bean
    public ListDirectoryTool listDirectoryTool() {
        return new ListDirectoryTool();
    }

    @Bean
    public CreateDirectoryTool createDirectoryTool() {
        return new CreateDirectoryTool();
    }

    @Bean
    public WriteFileTool writeFileTool() {
        return new WriteFileTool();
    }

    @Bean
    public RunShellTool runShellTool() {
        return new RunShellTool();
    }

    @Bean
    public ToolRegistry toolRegistry(List<AxerTool> tools) {
        return new ToolRegistry(tools);
    }

    @Bean
    public ToolExecutor toolExecutor(ToolRegistry toolRegistry) {
        return new ToolExecutor(toolRegistry);
    }

    @Bean
    public ConversationAgent conversationAgent(
            LlmProvider llmProvider,
            ToolExecutor toolExecutor,
            ToolRegistry toolRegistry,
            AxerCodeAgentProperties agentProperties
    ) {
        return new ToolCallingAgent(
                llmProvider,
                toolExecutor,
                toolRegistry,
                agentProperties.getMaxToolRounds(),
                new SessionContextWindow(agentProperties.getMaxRecentMessages()),
                agentProperties.getMaxReflectionRounds()
        );
    }
}
