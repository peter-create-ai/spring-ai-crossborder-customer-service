package com.omnimerchant.tenant.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户上下文持有者，通过 TransmittableThreadLocal 传递租户 ID。
 * 支持子线程透传（线程池场景），请求进入时设置，结束时清理。
 */
public final class TenantContextHolder {

    private static final TransmittableThreadLocal<Long> TENANT_ID = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<Boolean> TENANT_FILTER_DISABLED = new TransmittableThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(Long tenantId) {
        TENANT_ID.set(tenantId);
    }

    public static Long get() {
        return TENANT_ID.get();
    }

    /**
     * Explicitly disables tenant SQL filtering for trusted admin/system flows.
     *
     * <p>This must never be used for customer-facing APIs. Callers that enable
     * the bypass are responsible for clearing the context in a finally block.</p>
     */
    public static void disableTenantFilter() {
        TENANT_FILTER_DISABLED.set(Boolean.TRUE);
    }

    public static boolean isTenantFilterDisabled() {
        return Boolean.TRUE.equals(TENANT_FILTER_DISABLED.get());
    }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_FILTER_DISABLED.remove();
    }
}
