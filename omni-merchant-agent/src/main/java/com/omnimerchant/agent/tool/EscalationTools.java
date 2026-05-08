package com.omnimerchant.agent.tool;

import com.omnimerchant.agent.escalation.EscalationResult;
import com.omnimerchant.agent.escalation.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * Spring AI Tool: human agent escalation.
 * LLM calls escalateToHuman when it cannot resolve the issue.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EscalationTools {

    private final EscalationService escalationService;

    @Tool(description = """
            Escalate the conversation to a human customer service agent. \
            Use this tool when: the customer explicitly requests to speak to a human, \
            the AI cannot resolve the issue after multiple attempts, \
            the issue involves high-value or sensitive matters (amount > $100 in dispute), \
            the customer sentiment is strongly negative or angry, \
            or the confidence in the answer is below 75%. \
            Returns a ticket ID and estimated wait time.
            """)
    public EscalationResult escalateToHuman(
            @ToolParam(description = "Primary reason for escalation (e.g., cannot resolve, customer request, high value dispute)")
            String reason,
            @ToolParam(description = "Brief summary of the customer's issue and what has been attempted so far")
            String summary,
            @ToolParam(description = "Priority level: 1=low, 2=medium, 3=high, 4=urgent")
            int priority) {
        try {
            log.info("escalateToHuman: reason='{}', priority={}", reason, priority);
            return escalationService.escalate(reason, summary, priority);
        } catch (Exception e) {
            log.error("escalateToHuman failed: {}", e.getMessage());
            return new EscalationResult("ERROR", 0, "FAILED",
                    "Escalation failed: " + e.getMessage());
        }
    }
}
