package cn.edu.tjut.huaxin.product.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Creates and shares the thread-safe SqlSessionFactory.
 */
public final class MyBatisUtil {
    private static final SqlSessionFactory SQL_SESSION_FACTORY = buildFactory();

    private MyBatisUtil() {
    }

    private static SqlSessionFactory buildFactory() {
        try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
            return MyBatisPlusFactoryBuilder.build(inputStream);
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static SqlSession openSession() {
        return SQL_SESSION_FACTORY.openSession();
    }
}
