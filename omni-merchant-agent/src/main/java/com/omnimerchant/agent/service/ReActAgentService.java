package com.omnimerchant.agent.service;

import com.omnimerchant.agent.advisor.SafeGuardAdvisor;
import com.omnimerchant.agent.advisor.TokenUsageAdvisor;
import com.omnimerchant.agent.context.CallContextHolder;
import com.omnimerchant.agent.language.MultiLingualEngine;
import com.omnimerchant.agent.memory.RedisChatMemory;
import com.omnimerchant.agent.router.ModelRouter;
import com.omnimerchant.tenant.context.TenantContextHolder;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Supplier;

/**
 * Core ReAct Agent service — the heart of OmniMerchant.
 *
 * Flow:
 * 1. Validate input (SafeGuardAdvisor)
 * 2. Preprocess (detect language, translate non-EN → EN)
 * 3. Route to best model based on intent
 * 4. Build ChatClient with all tools + advisors + memory
 * 5. Stream response via ChatClient.prompt().stream()
 * 6. Postprocess (translate EN → original language)
 * 7. Save to chat memory
 */
@Slf4j
@Service
public class ReActAgentService {

    private final MultiLingualEngine multiLingualEngine;
    private final ModelRouter modelRouter;
    private final RedisChatMemory chatMemory;
    private final SafeGuardAdvisor safeGuardAdvisor;
    private final TokenUsageAdvisor tokenUsageAdvisor;
    private final ToolCallbackProvider toolCallbackProvider;
    private final CircuitBreaker llmCircuitBreaker;

    private static final String SYSTEM_PROMPT = """
            You are an intelligent customer service agent for cross-border e-commerce.

            Decision Process (ReAct):
            1. THINK about what the customer needs
            2. ACT by calling appropriate tools
            3. OBSERVE results and decide the next step
            4. Repeat until you can give a complete answer

            Rules (CRITICAL):
            - NEVER fabricate order numbers, tracking numbers, or policy details
            - ALWAYS use tools to get real data
            - If confidence < 75%, escalate to a human agent
            - If the amount in dispute > $100, escalate
            - If the customer is angry or frustrated, escalate
            - Be concise, polite, and professional
            - Respond in the customer's language
            """;

    public ReActAgentService(
            MultiLingualEngine multiLingualEngine,
            ModelRouter modelRouter,
            RedisChatMemory chatMemory,
            SafeGuardAdvisor safeGuardAdvisor,
            TokenUsageAdvisor tokenUsageAdvisor,
            ToolCallbackProvider toolCallbackProvider,
            CircuitBreaker llmCircuitBreaker) {
        this.multiLingualEngine = multiLingualEngine;
        this.modelRouter = modelRouter;
        this.chatMemory = chatMemory;
        this.safeGuardAdvisor = safeGuardAdvisor;
        this.tokenUsageAdvisor = tokenUsageAdvisor;
        this.toolCallbackProvider = toolCallbackProvider;
        this.llmCircuitBreaker = llmCircuitBreaker;
    }

    /**
     * Core streaming chat method.
     *
     * @param conversationUuid unique conversation identifier
     * @param userMessage      raw user input (any language)
     * @param intent           detected intent (ORDER_QUERY, REFUND_POLICY, etc.)
     * @return Flux of text chunks (SSE-compatible)
     */
    public Flux<String> chat(Long tenantId, String conversationUuid, String userMessage, String intent) {
        return Flux.defer(() -> {
            // Set tenant context for tool calls (must propagate to reactor threads)
            TenantContextHolder.set(tenantId);

            // Step 1: Input validation
            var rejection = safeGuardAdvisor.validate(userMessage);
            if (rejection != null) {
                return Flux.just("[SAFEGUARD] " + rejection);
            }
            var safeInput = safeGuardAdvisor.maskPii(userMessage);

            // Step 2: Language preprocessing
            var processed = multiLingualEngine.preprocess(safeInput);
            var enText = processed.getTranslatedText();
            var originalLang = processed.getDetectedLanguage();

            // Step 3: Model routing
            var routed = modelRouter.route(intent);

            // Step 4: Build ChatClient
            var chatClient = ChatClient.builder(routed.chatModel())
                    .defaultTools(toolCallbackProvider.getToolCallbacks())
                    .defaultSystem(SYSTEM_PROMPT)
                    .build();

            // Step 5: Save user message to memory
            chatMemory.add(conversationUuid, List.of(new UserMessage(enText)));

            // Step 6: Call with circuit breaker, stream
            var fullResponse = new StringBuilder();
            Supplier<Flux<String>> streamSupplier = () -> {
                CallContextHolder.set(intent, conversationUuid);
                return chatClient.prompt()
                        .user(enText)
                        .stream()
                        .content()
                        .doFinally(signalType -> CallContextHolder.clear());
            };

            return streamWithBreaker(streamSupplier, routed.modelName())
                    .doOnNext(fullResponse::append)
                    .doOnComplete(() -> {
                        // Step 7: Postprocess and save
                        var responseText = fullResponse.toString();
                        String finalText;
                        if (processed.isNeedsTranslation()) {
                            finalText = multiLingualEngine.postprocess(responseText, originalLang);
                        } else {
                            finalText = responseText;
                        }
                        chatMemory.add(conversationUuid, List.of(new AssistantMessage(finalText)));
                        log.info("Chat complete: conv={}, intent={}, model={}, responseLen={}",
                                conversationUuid, intent, routed.modelName(), finalText.length());
                    })
                    .onErrorResume(e -> {
                        log.error("Chat failed for conv={}: {}", conversationUuid, e.getMessage());
                        // Try fallback model
                        return Flux.just("[FALLBACK] I'm having trouble processing your request. "
                                + "Please try again or ask to speak with a human agent.");
                    });
        }).doFinally(signalType -> TenantContextHolder.clear());
    }

    private Flux<String> streamWithBreaker(Supplier<Flux<String>> supplier, String modelName) {
        try {
            var decorated = llmCircuitBreaker.decorateSupplier(() -> {
                var start = System.currentTimeMillis();
                // For streaming, we return a Flux — but circuit breaker expects a synchronous result
                // We wrap the first emission to check breaker state
                return supplier.get();
            });
            return decorated.get();
        } catch (Exception e) {
            log.warn("Circuit breaker prevented call to {}, using fallback", modelName);
            return Flux.just("[CIRCUIT_OPEN] Service temporarily unavailable. Please try again later.");
        }
    }
}
