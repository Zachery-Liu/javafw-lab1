package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import java.util.Arrays;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** 原生 MyBatis 基础测试：6 项，每项只演示一种常用操作。 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductMapperTest extends AbstractH2MapperTest {
    /** 查询：按 ID 找到商品，核对编码。 */
    @Test
    public void test01_selectById() {
        ConsoleOutput.operation("按编号 1 查询商品，核对商品编码。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Product product = ConsoleOutput.product("按 ID 查询", session.getMapper(ProductMapper.class).selectById(1L));
            Assert.assertNotNull(product);
            Assert.assertEquals("TEST-001", product.getProductCode());
        }
    }

    /** 新增：提交商品，检查影响行数和回填的主键，再按主键查回。 */
    @Test
    public void test02_insert() {
        ConsoleOutput.operation("新增商品、提交事务，再按回填主键查询。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Product product = newProduct("NEW-001");
            ConsoleOutput.product("待新增商品", product);
            Assert.assertEquals(1, ConsoleOutput.rows("新增商品", mapper.insert(product)));
            session.commit();
            ConsoleOutput.info("新增事务已提交，回填主键=" + product.getProductId());
            Assert.assertNotNull(product.getProductId());
            Assert.assertNotNull(ConsoleOutput.product("新增后查询", mapper.selectById(product.getProductId())));
        }
    }

    /** 动态修改：只传名称及查询得到的版本，其他业务字段保持原值。 */
    @Test
    public void test03_updateName() {
        ConsoleOutput.operation("只修改商品名称，检查库存保持不变。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Product original = ConsoleOutput.product("修改前", mapper.selectById(1L));
            Product changes = new Product();
            changes.setProductId(original.getProductId());
            changes.setVersion(original.getVersion());
            changes.setProductName("新键盘名称");
            Assert.assertEquals(1, ConsoleOutput.rows("动态修改名称", mapper.updateDynamic(changes)));
            session.commit();
            ConsoleOutput.info("修改事务已提交。");
            Product updated = ConsoleOutput.product("修改后", mapper.selectById(1L));
            Assert.assertEquals("新键盘名称", updated.getProductName());
            Assert.assertEquals(original.getStockQuantity(), updated.getStockQuantity());
            ConsoleOutput.info("库存对比：修改前=" + original.getStockQuantity() + "，修改后=" + updated.getStockQuantity());
        }
    }

    /** 逻辑删除：设置 deleted 标记，之后普通查询返回 null。 */
    @Test
    public void test04_delete() {
        ConsoleOutput.operation("逻辑删除编号 3，检查普通查询不可见。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Assert.assertEquals(1, ConsoleOutput.rows("逻辑删除", mapper.deleteById(3L)));
            session.commit();
            ConsoleOutput.info("删除事务已提交。");
            Assert.assertNull(ConsoleOutput.product("删除后查询", mapper.selectById(3L)));
        }
    }

    /** 动态查询：if/where 组合名称和状态两个条件，找到在售键盘。 */
    @Test
    public void test05_selectByCondition() {
        ConsoleOutput.operation("组合名称和在售状态两个条件查询。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductQuery query = new ProductQuery();
            query.setProductName("键盘");
            query.setStatus(1);
            ConsoleOutput.criteria(query);
            List<Product> products = ConsoleOutput.products("条件查询结果",
                    session.getMapper(ProductMapper.class).selectByCondition(query));
            Assert.assertEquals(1, products.size());
            Assert.assertEquals("TEST-001", products.get(0).getProductCode());
        }
    }

    /** 批量删除：foreach 把两个 ID 展开成 IN 参数，只删除选中的商品。 */
    @Test
    public void test06_deleteBatch() {
        ConsoleOutput.operation("批量逻辑删除编号 1、3，保留未选中的编号 2。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Assert.assertEquals(2, ConsoleOutput.rows("批量删除", mapper.deleteBatch(Arrays.asList(1L, 3L))));
            session.commit();
            ConsoleOutput.info("批量删除事务已提交。");
            List<Product> remaining = ConsoleOutput.products("剩余可见商品", mapper.selectAll());
            Assert.assertEquals(1, remaining.size());
            Assert.assertEquals("TEST-002", remaining.get(0).getProductCode());
        }
    }
}
