package com.omnimerchant.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.omnimerchant.tenant.context.TenantContextHolder;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;

import java.util.Set;

/**
 * TenantLineHandler that fails closed for tenant-scoped SQL.
 *
 * <p>Tables in {@code ignoreTables} and trusted admin/system flows may bypass
 * tenant filtering. All other tenant-scoped SQL must have a tenant id in
 * {@link TenantContextHolder}; otherwise an exception is raised instead of
 * generating unscoped SQL.</p>
 */
final class FailClosedTenantLineHandler implements TenantLineHandler {

    private final Set<String> ignoreTables;

    FailClosedTenantLineHandler(Set<String> ignoreTables) {
        this.ignoreTables = ignoreTables;
    }

    @Override
    public Expression getTenantId() {
        var tenantId = TenantContextHolder.get();
        if (tenantId != null) {
            return new LongValue(tenantId);
        }
        if (TenantContextHolder.isTenantFilterDisabled()) {
            return null;
        }
        throw new IllegalStateException(
                "Missing tenant context for tenant-scoped SQL. " +
                "Set X-Tenant-Id on request or explicitly disable tenant filter for trusted admin/system flows.");
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return ignoreTables.contains(tableName.toLowerCase())
                || TenantContextHolder.isTenantFilterDisabled();
    }
}
