package com.omnimerchant.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.omnimerchant.tenant.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * MyBatis-Plus 全局配置：
 * - 分页插件（MySQL）
 * - 多租户插件（自动注入 tenant_id）
 * - 自动填充（created_at / updated_at）
 */
@Slf4j
@Configuration
@MapperScan("com.omnimerchant.**.mapper")
public class MybatisPlusConfig {

    private static final Set<String> IGNORE_TABLES = Set.of(
            "tenant",
            "token_usage_daily",
            "human_agent",
            "escalation_record",
            "webhook_event",
            "rate_limit_record"
    );

    /**
     * 核心拦截器链。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        var interceptor = new MybatisPlusInterceptor();

        // 多租户 SQL 自动注入（在最外层，确保所有查询都带上 tenant_id）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                var tenantId = TenantContextHolder.get();
                if (tenantId != null) {
                    return new LongValue(tenantId);
                }
                // 无租户上下文时放行（查询 tenant 表本身等场景）
                return null;
            }

            @Override
            public String getTenantIdColumn() {
                return "tenant_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 这些表不需要 tenant_id 过滤
                return IGNORE_TABLES.contains(tableName.toLowerCase());
            }
        }));

        // 分页插件
        var pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }

    /**
     * 自动填充创建时间和更新时间。
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            @Override
            public void insertFill(MetaObject metaObject) {
                var now = LocalDateTime.now();
                this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
                this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
            }

            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
