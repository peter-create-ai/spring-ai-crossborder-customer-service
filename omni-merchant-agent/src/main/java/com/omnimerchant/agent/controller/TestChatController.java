package com.omnimerchant.agent.controller;

import com.omnimerchant.common.dto.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 测试 Controller：验证 LLM 连通性。
 */
@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestChatController {

    private final OpenAiChatModel openAiChatModel;

    public TestChatController(@Qualifier("openAiChatModel") OpenAiChatModel openAiChatModel) {
        this.openAiChatModel = openAiChatModel;
    }

    /**
     * 简单对话测试。
     * <pre>
     * POST /api/test/chat
     * {"message": "Hello, who are you?"}
     * </pre>
     */
    @PostMapping("/chat")
    public R<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        var userMessage = body.getOrDefault("message", "Say hello in one sentence.");
        var start = System.currentTimeMillis();

        var response = openAiChatModel.call(new Prompt(userMessage));
        var output = response.getResult().getOutput();
        var metadata = response.getMetadata();

        var elapsed = System.currentTimeMillis() - start;
        log.info("LLM call: latency={}ms, model={}, promptTokens={}, completionTokens={}",
                elapsed, metadata.getModel(),
                metadata.getUsage().getPromptTokens(),
                metadata.getUsage().getGenerationTokens());

        return R.ok(Map.of(
                "reply", output.getText(),
                "model", metadata.getModel(),
                "promptTokens", metadata.getUsage().getPromptTokens(),
                "completionTokens", metadata.getUsage().getGenerationTokens(),
                "latencyMs", elapsed
        ));
    }
}
