package cn.edu.tjut.huaxin.product.util;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.MybatisXMLConfigBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import org.apache.ibatis.session.SqlSessionFactory;

import java.io.InputStream;

/** 普通 Maven 项目的工厂构建工具，不依赖 Spring。 生产与 H2 测试都通过此入口注册两套 Mapper 和分页插件。 */
public final class MyBatisPlusFactoryBuilder {
    private MyBatisPlusFactoryBuilder() {}

    public static SqlSessionFactory build(InputStream inputStream) {
        // MP 构建器会处理 BaseMapper 通用 SQL 的注入，同时支持原生 XML 映射。
        MybatisConfiguration configuration =
                (MybatisConfiguration) new MybatisXMLConfigBuilder(inputStream).parse();
        GlobalConfig global = GlobalConfigUtils.getGlobalConfig(configuration);
        global.setMetaObjectHandler(new ProductMetaObjectHandler());
        GlobalConfigUtils.setGlobalConfig(configuration, global);

        // 外层是 MP 插件容器，内层分页拦截器负责计数和 LIMIT SQL 改写。
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new ProductVersionGuardInterceptor());
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);
        // 必须加入实际工厂；在打开任何 SqlSession 前完成注册。
        configuration.addInterceptor(interceptor);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }
}
