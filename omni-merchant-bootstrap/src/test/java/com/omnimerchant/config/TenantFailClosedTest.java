package com.omnimerchant.config;

import com.omnimerchant.common.constant.Constants;
import com.omnimerchant.tenant.context.TenantContextHolder;
import com.omnimerchant.tenant.interceptor.TenantInterceptor;
import net.sf.jsqlparser.expression.LongValue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantFailClosedTest {

    private final FailClosedTenantLineHandler tenantLineHandler =
            new FailClosedTenantLineHandler(Set.of("tenant", "token_usage_daily"));
    private final TenantInterceptor tenantInterceptor = new TenantInterceptor();

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void tenantLineHandlerShouldUseTenantContextWhenPresent() {
        TenantContextHolder.set(42L);

        var expression = tenantLineHandler.getTenantId();

        assertThat(expression).isInstanceOf(LongValue.class);
        assertThat(expression.toString()).isEqualTo("42");
        assertThat(tenantLineHandler.ignoreTable("chat_message")).isFalse();
    }

    @Test
    void tenantLineHandlerShouldFailClosedWithoutTenantContext() {
        assertThatThrownBy(tenantLineHandler::getTenantId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing tenant context");
    }

    @Test
    void tenantLineHandlerShouldAllowExplicitAdminBypass() {
        TenantContextHolder.disableTenantFilter();

        assertThat(tenantLineHandler.getTenantId()).isNull();
        assertThat(tenantLineHandler.ignoreTable("chat_message")).isTrue();
    }

    @Test
    void tenantLineHandlerShouldIgnoreConfiguredGlobalTables() {
        assertThat(tenantLineHandler.ignoreTable("tenant")).isTrue();
        assertThat(tenantLineHandler.ignoreTable("token_usage_daily")).isTrue();
        assertThat(tenantLineHandler.ignoreTable("conversation")).isFalse();
    }

    @Test
    void tenantInterceptorShouldRejectMissingTenantHeader() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/chat/stream");
        var response = new MockHttpServletResponse();

        var proceed = tenantInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void tenantInterceptorShouldRejectInvalidTenantHeader() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/chat/stream");
        request.addHeader(Constants.HEADER_TENANT_ID, "abc");
        var response = new MockHttpServletResponse();

        var proceed = tenantInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void tenantInterceptorShouldRejectNonPositiveTenantHeader() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/chat/stream");
        request.addHeader(Constants.HEADER_TENANT_ID, "0");
        var response = new MockHttpServletResponse();

        var proceed = tenantInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isFalse();
        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void tenantInterceptorShouldAcceptValidTenantHeader() throws Exception {
        var request = new MockHttpServletRequest("POST", "/api/chat/stream");
        request.addHeader(Constants.HEADER_TENANT_ID, "42");
        var response = new MockHttpServletResponse();

        var proceed = tenantInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.get()).isEqualTo(42L);
    }

    @Test
    void tenantInterceptorShouldAllowPublicPathsWithoutTenantHeader() throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/health");
        var response = new MockHttpServletResponse();

        var proceed = tenantInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.get()).isNull();
    }

    @Test
    void tenantInterceptorShouldAllowTrustedAdminBypass() throws Exception {
        TenantContextHolder.disableTenantFilter();
        var request = new MockHttpServletRequest("GET", "/api/conversations");
        var response = new MockHttpServletResponse();

        var proceed = tenantInterceptor.preHandle(request, response, new Object());

        assertThat(proceed).isTrue();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(TenantContextHolder.get()).isNull();
    }
}
