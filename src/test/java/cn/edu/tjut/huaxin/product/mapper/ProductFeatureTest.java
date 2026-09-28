package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** 保留三个直观的功能案例：三表联查、乐观锁、一级缓存。 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductFeatureTest extends AbstractH2MapperTest {
    /** 三表查询：商品 JOIN 分类和品牌，返回对象直接带有分类名、品牌名。 */
    @Test
    public void test01_joinCategoryAndBrand() {
        ConsoleOutput.operation("三表联查：商品同时带出分类名和品牌名。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Product product = ConsoleOutput.product("商品、分类、品牌联查",
                    session.getMapper(ProductMapper.class).selectById(1L));
            Assert.assertNotNull(product);
            Assert.assertEquals("数码配件", product.getCategory());
            Assert.assertEquals("TestBrand", product.getBrand());
        }
    }

    /** 乐观锁：两个会话读取同一版本，先提交者成功，后提交的旧版本影响 0 行。 */
    @Test
    public void test02_optimisticLock() {
        ConsoleOutput.operation("两个会话读取同一版本，先提交者成功，旧版本更新失败。");
        try (SqlSession first = sqlSessionFactory.openSession();
             SqlSession second = sqlSessionFactory.openSession()) {
            ProductPlusMapper firstMapper = first.getMapper(ProductPlusMapper.class);
            ProductPlusMapper secondMapper = second.getMapper(ProductPlusMapper.class);
            Product firstCopy = ConsoleOutput.product("会话一读取", firstMapper.selectById(1L));
            Product secondCopy = ConsoleOutput.product("会话二读取", secondMapper.selectById(1L));
            firstCopy.setProductName("先提交的名称");
            Assert.assertEquals(1, ConsoleOutput.rows("会话一更新", firstMapper.updateById(firstCopy)));
            first.commit();
            ConsoleOutput.info("会话一已提交，新版本=" + firstCopy.getVersion());
            secondCopy.setProductName("旧版本想覆盖的名称");
            Assert.assertEquals(0, ConsoleOutput.rows("会话二用旧版本更新", secondMapper.updateById(secondCopy)));
            second.commit();
            second.clearCache();
            Product stored = ConsoleOutput.product("刷新后数据库中的商品", secondMapper.selectById(1L));
            Assert.assertEquals("先提交的名称", stored.getProductName());
            ConsoleOutput.info("旧版本影响 0 行，数据库保留先提交的名称。");
        }
    }

    /** 一级缓存：同一个会话内重复查询同一 ID，返回缓存中的同一个对象。 */
    @Test
    public void test03_firstLevelCache() {
        ConsoleOutput.operation("同一个会话中，连续两次按编号 1 查询商品。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Product first = ConsoleOutput.product("第一次查询", mapper.selectById(1L));
            Product second = ConsoleOutput.product("第二次查询", mapper.selectById(1L));
            Assert.assertNotNull(first);
            Assert.assertSame(first, second);
            ConsoleOutput.info("两次结果是否为同一对象=" + (first == second) + "；观察 SQL 日志只有一次 SELECT。");
        }
    }
}
