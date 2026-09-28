package cn.edu.tjut.huaxin.product.util;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * 生产数据库入口：复用线程安全的工厂，每次打开独立会话。
 * SqlSession 非线程安全，调用者用完必须关闭，不应跨线程共享。
 */
public final class MyBatisUtil {
    // 类初始化时构建一次生产工厂；final 限制重新赋值，不代表内部配置不可改变。
    private static final SqlSessionFactory SQL_SESSION_FACTORY = buildFactory();

    private MyBatisUtil() {
    }

    private static SqlSessionFactory buildFactory() {
        // 从运行时资源路径读取配置；try-with-resources 在退出时关闭输入流。
        try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml")) {
            return MyBatisPlusFactoryBuilder.build(inputStream);
        } catch (IOException exception) {
            // 配置无法读取时终止工具类初始化，不返回一个无效工厂。
            throw new ExceptionInInitializerError(exception);
        }
    }

    /** 默认手动提交：写操作需要 commit；调用方使用 try-with-resources 关闭会话。 */
    public static SqlSession openSession() {
        return SQL_SESSION_FACTORY.openSession();
    }
}
