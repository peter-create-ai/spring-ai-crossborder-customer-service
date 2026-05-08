package com.omnimerchant.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Spring AI Tool: order query.
 * Stub implementation — will be backed by OrderInfoService in future sprint.
 */
@Slf4j
@Component
public class OrderTools {

    @Tool(description = """
            Query detailed order information by order ID. \
            Returns order status, items, total amount, tracking number, and shipping address. \
            Use this tool when the customer asks about a specific order — \
            status inquiry, delivery ETA, order contents, or shipping details.
            """)
    public OrderQueryResult queryOrder(
            @ToolParam(description = "Order ID, format like #12345 or ORD-XXXXX")
            String orderId,
            @ToolParam(description = "Customer email for identity verification", required = false)
            String customerEmail) {
        log.info("queryOrder stub: orderId={}, email={}", orderId, customerEmail);
        try {
            return new OrderQueryResult(
                    orderId,
                    "PROCESSING",
                    List.of(Map.of("name", "Sample Item", "qty", 1, "price", "$29.99")),
                    "$29.99",
                    "TRK-" + orderId.replaceAll("[^A-Za-z0-9]", ""),
                    "123 Main St, City, Country"
            );
        } catch (Exception e) {
            log.error("queryOrder failed: orderId={}, error={}", orderId, e.getMessage());
            throw new RuntimeException("订单查询失败", e);
        }
    }

    public record OrderQueryResult(
            String orderId,
            String status,
            List<Map<String, Object>> items,
            String totalAmount,
            String trackingNumber,
            String shippingAddress) {
    }
}
