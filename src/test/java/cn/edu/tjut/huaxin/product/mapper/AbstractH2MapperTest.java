package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.util.MyBatisPlusFactoryBuilder;
import cn.edu.tjut.huaxin.product.entity.Product;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;

/**
 * H2 测试的公共准备代码，本类不是可以单独运行的测试用例。
 * 子类继承工厂初始化、每项测试前的数据重建、商品对象构造和中文输出规则。
 * 顺序只方便阅读和演示；任意一个 @Test 都能单独运行，不依赖前项写入的数据。
 */
public abstract class AbstractH2MapperTest {
    // SqlSessionFactory 创建会话；每个具体测试类开始前重新构建，不共享 SqlSession。
    protected static SqlSessionFactory sqlSessionFactory;

    // 非测试规则：打印每项开始、通过或失败；只有全部断言完成才提示通过。
    @Rule
    public final TestWatcher consoleOutput = ConsoleOutput.watch();

    /**
     * 非测试方法：JUnit 在每个具体测试类开始前自动调用一次（@BeforeClass）。
     * 只读取 H2 配置，注册 Mapper、版本校验、乐观锁和分页插件，不连接真实 MySQL。
     * 工厂供本类全部测试创建各自的会话；try-with-resources 会关闭配置文件流。
     */
    @BeforeClass
    public static void buildSqlSessionFactory() throws Exception {
        try (InputStream input = Resources.getResourceAsStream("mybatis-config-test.xml")) {
            sqlSessionFactory = MyBatisPlusFactoryBuilder.build(input);
        }
    }

    /**
     * 非测试方法：JUnit 在每一个 @Test 之前自动调用（@Before）。
     * 重建商品及关联表，恢复基准商品、分类、品牌及关联样例，版本为 0。
     * 提交准备数据后再运行测试；这样删除、修改、回滚都不会影响下一项测试。
     * SQL 出错立即抛出异常，避免把准备失败误当成业务测试失败。
     */
    @Before
    public void resetDatabase() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession();
             Reader reader = Resources.getResourceAsReader("schema-h2.sql")) {
            ScriptRunner runner = new ScriptRunner(session.getConnection());
            runner.setLogWriter(null);
            runner.setErrorLogWriter(null);
            runner.setStopOnError(true);
            runner.runScript(reader);
            session.commit();
        }
    }

    /**
     * 非测试辅助方法：供两个新增测试构造商品对象，本方法不访问数据库。
     * 分类 1、品牌 1 来自准备脚本；真正写库和提交由调用它的 @Test 完成。
     */
    protected Product newProduct(String code) {
        Product product = new Product();
        product.setProductCode(code);
        product.setProductName("新增商品");
        product.setCategoryId(1L);
        product.setBrandId(1L);
        product.setUnit("件");
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setStockQuantity(10);
        product.setMinStock(1);
        product.setStatus(1);
        return product;
    }
}
