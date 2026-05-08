package com.omnimerchant.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Spring AI Tool: logistics tracking.
 * Stub implementation — will be backed by logistics API in future sprint.
 */
@Slf4j
@Component
public class LogisticsTools {

    @Tool(description = """
            Track a shipment by tracking number. \
            Returns current status, estimated delivery date, and checkpoint history. \
            Use this tool when the customer asks about shipping status, \
            delivery ETA, or package location.
            """)
    public LogisticsResult trackLogistics(
            @ToolParam(description = "Tracking number from the carrier (e.g., FedEx, UPS, DHL)")
            String trackingNumber) {
        log.info("trackLogistics stub: trackingNumber={}", trackingNumber);
        try {
            return new LogisticsResult(
                    trackingNumber,
                    "IN_TRANSIT",
                    "2026-05-10",
                    List.of(
                            new Checkpoint("2026-05-06", "Package picked up", "Origin City"),
                            new Checkpoint("2026-05-07", "In transit", "Distribution Center"),
                            new Checkpoint("2026-05-09", "Out for delivery", "Local Facility")
                    )
            );
        } catch (Exception e) {
            log.error("trackLogistics failed: trackingNumber={}, error={}", trackingNumber, e.getMessage());
            throw new RuntimeException("物流查询失败", e);
        }
    }

    public record LogisticsResult(
            String trackingNumber,
            String status,
            String estimatedDelivery,
            List<Checkpoint> checkpoints) {
    }

    public record Checkpoint(
            String date,
            String status,
            String location) {
    }
}
