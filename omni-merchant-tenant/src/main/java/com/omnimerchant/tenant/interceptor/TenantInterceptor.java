package com.omnimerchant.tenant.interceptor;

import com.omnimerchant.common.constant.Constants;
import com.omnimerchant.tenant.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

/**
 * 多租户拦截器：从请求头 X-Tenant-Id 提取租户 ID 并设置到 ThreadLocal。
 * 对于不需要租户的接口（/api/health 等），允许跳过。
 */
@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Set<String> EXCLUDE_PATHS = Set.of(
            "/api/health",
            "/api/tenants"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestPath = request.getRequestURI();
        
        if (EXCLUDE_PATHS.stream().anyMatch(requestPath::startsWith)) {
            return true;
        }

        var tenantIdStr = request.getHeader(Constants.HEADER_TENANT_ID);
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            log.warn("请求缺少租户ID: {}", requestPath);
            return true;
        }

        try {
            TenantContextHolder.set(Long.parseLong(tenantIdStr));
        } catch (NumberFormatException e) {
            log.warn("无效的租户ID格式: {}，路径: {}", tenantIdStr, requestPath);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
