package com.omnimerchant.agent.router;

import com.omnimerchant.agent.advisor.TokenUsageAdvisor;
import com.omnimerchant.agent.ratelimit.RateLimitedChatModel;
import com.omnimerchant.agent.ratelimit.TokenRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Routes requests to the optimal chat model based on intent type and complexity.
 * All models are wrapped with RateLimitedChatModel for 3-layer rate limiting.
 */
@Slf4j
@Service
public class ModelRouter {

    private final ChatModel openAiModel;
    private final ChatModel anthropicModel;
    private final ChatModel deepSeekModel;

    public ModelRouter(
            @Qualifier("openAiChatModel") OpenAiChatModel openAiChatModel,
            @Qualifier("anthropicChatModel") AnthropicChatModel anthropicChatModel,
            @Qualifier("deepSeekChatModel") OpenAiChatModel deepSeekChatModel,
            TokenRateLimiter rateLimiter,
            TokenUsageAdvisor tokenUsageAdvisor) {
        this.openAiModel = new RateLimitedChatModel(openAiChatModel, rateLimiter, tokenUsageAdvisor, "gpt-4o-mini");
        this.anthropicModel = new RateLimitedChatModel(anthropicChatModel, rateLimiter, tokenUsageAdvisor, "claude-haiku-4-5");
        this.deepSeekModel = new RateLimitedChatModel(deepSeekChatModel, rateLimiter, tokenUsageAdvisor, "deepseek-chat");
    }

    public RoutedModel route(String intent) {
        var model = selectModel(intent);
        log.info("ModelRouter: intent={} → model={}", intent, model.modelName());
        return model;
    }

    public RoutedModel fallback() {
        return new RoutedModel(deepSeekModel, "deepseek", "deepseek-chat");
    }

    private RoutedModel selectModel(String intent) {
        if (intent == null) {
            return new RoutedModel(openAiModel, "openai", "gpt-4o-mini");
        }
        return switch (intent.toUpperCase()) {
            case "COMPLAINT", "ESCALATION" ->
                    new RoutedModel(anthropicModel, "anthropic", "claude-haiku-4-5");
            case "ORDER_QUERY", "LOGISTICS_TRACKING",
                 "REFUND_POLICY", "PRODUCT_INQUIRY",
                 "GREETING", "UNCLEAR" ->
                    new RoutedModel(openAiModel, "openai", "gpt-4o-mini");
            default ->
                    new RoutedModel(openAiModel, "openai", "gpt-4o-mini");
        };
    }

    public record RoutedModel(ChatModel chatModel, String providerName, String modelName) {
    }
}
