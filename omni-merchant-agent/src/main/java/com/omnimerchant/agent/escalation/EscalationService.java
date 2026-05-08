package com.omnimerchant.agent.escalation;

import com.omnimerchant.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Human agent escalation service.
 * Creates escalation tickets and estimates wait time based on priority.
 * Currently in-memory mock — will be backed by escalation_record table in future sprint.
 */
@Slf4j
@Service
public class EscalationService {

    public EscalationResult escalate(String reason, String summary, int priority) {
        var tenantId = TenantContextHolder.get();
        var ticketId = "ESC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        var waitMinutes = estimateWaitMinutes(priority);
        log.info("Escalation created: ticketId={}, tenantId={}, priority={}, reason={}",
                ticketId, tenantId, priority, reason);
        return new EscalationResult(ticketId, waitMinutes, "QUEUED",
                "Your case has been escalated to a human agent. " +
                "Estimated wait time: " + waitMinutes + " minutes. " +
                "Reference: " + ticketId);
    }

    private int estimateWaitMinutes(int priority) {
        var baseWait = switch (priority) {
            case 4 -> 1;
            case 3 -> 3;
            case 2 -> 8;
            default -> 15;
        };
        return baseWait + ThreadLocalRandom.current().nextInt(0, 5);
    }
}
