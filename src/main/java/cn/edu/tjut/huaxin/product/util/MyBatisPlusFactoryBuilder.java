package cn.edu.tjut.huaxin.product.util;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.InputStream;

/**
 * Builds the non-Spring SqlSessionFactory used by both MyBatis mapper styles.
 */
public final class MyBatisPlusFactoryBuilder {
    private MyBatisPlusFactoryBuilder() {
    }

    public static SqlSessionFactory build(InputStream inputStream) {
        SqlSessionFactory factory =
                new MybatisSqlSessionFactoryBuilder().build(inputStream);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(
                new PaginationInnerInterceptor(DbType.MYSQL));
        factory.getConfiguration().addInterceptor(interceptor);
        return factory;
    }
}
