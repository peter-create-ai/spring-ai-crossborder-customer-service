package com.omnimerchant.admin.filter;

import com.omnimerchant.common.dto.R;
import com.omnimerchant.common.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
@Order(1)
@RequiredArgsConstructor
public class AdminAuthFilter implements Filter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/health",
            "/api/admin/login"
    );

    private static final Set<String> ADMIN_PATHS = Set.of(
            "/api/tenants",
            "/api/knowledge",
            "/api/conversations",
            "/api/billing",
            "/api/admin"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Allow CORS preflight
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(req, res);
            return;
        }

        // Public paths — no auth needed
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            chain.doFilter(req, res);
            return;
        }

        // Admin-protected paths — require JWT
        if (ADMIN_PATHS.stream().anyMatch(path::startsWith)) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                writeUnauthorized(response, "缺少认证令牌");
                return;
            }
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                writeUnauthorized(response, "令牌无效或已过期");
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(401);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                R.fail("401", message)
        ));
    }
}
