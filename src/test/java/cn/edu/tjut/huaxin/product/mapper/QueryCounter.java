package cn.edu.tjut.huaxin.product.mapper;

import java.sql.Connection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.junit.Assert;

/** 非测试辅助类：统计真正准备执行的 SELECT，不把缓存命中算作访问数据库。 */
@Intercepts(@Signature(type = StatementHandler.class, method = "prepare",
        args = {Connection.class, Integer.class}))
public final class QueryCounter implements Interceptor {
    private final AtomicInteger selects = new AtomicInteger();

    /** 非业务回调：在 JDBC 准备语句的位置计数，适用于缓存和延迟加载验证。 */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler handler = (StatementHandler) invocation.getTarget();
        if (handler.getBoundSql().getSql().trim().toUpperCase(Locale.ROOT).startsWith("SELECT")) {
            selects.incrementAndGet();
        }
        return invocation.proceed();
    }

    /** 非测试方法：数据准备完成后清零，避免准备阶段影响断言。 */
    public void reset() {
        selects.set(0);
    }

    /** 非测试方法：输出并断言累计查询次数，不主动执行查询。 */
    public void expect(String label, int expected) {
        ConsoleOutput.info(label + "；实际执行 SELECT 次数=" + selects.get());
        Assert.assertEquals(label, expected, selects.get());
    }
}
