package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.entity.Category;
import cn.edu.tjut.huaxin.product.entity.Brand;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.function.ThrowingRunnable;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** MyBatis-Plus：通用操作、完整筛选、分页、逻辑删除、时间填充和版本保护。 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductPlusMapperTest extends AbstractH2MapperTest {

    /** MP 新增与时间填充：验证主键、自动时间、手工时间保留及更新时间刷新。 */
    @Test
    public void testInsertAndAuditTimeFill() throws Exception {
        // BaseMapper 新增：不用手写 INSERT，检查主键回填和查询结果。
        {
            ConsoleOutput.operation("使用 MP 通用新增，提交后检查主键回填和查询结果。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                Product product = newProduct("PLUS-001");
                ConsoleOutput.product("待新增商品", product);
                Assert.assertEquals(1, ConsoleOutput.rows("MP 新增", mapper.insert(product)));
                session.commit();
                ConsoleOutput.info("新增事务已提交，回填主键=" + product.getProductId());
                Assert.assertNotNull(product.getProductId());
                Assert.assertNotNull(ConsoleOutput.product("MP 新增后查询", mapper.selectById(product.getProductId())));
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 时间填充：新增自动填充，手工值保留，更新仅刷新更新时间。
        {
            ConsoleOutput.operation("核对新增时间自动填充、手工时间保留和修改时间刷新。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                Product generated = newProduct("TIME-AUTO");
                Assert.assertEquals(1, mapper.insert(generated));
                Assert.assertNotNull(generated.getCreatedAt());
                Assert.assertNotNull(generated.getUpdatedAt());
                LocalDateTime fixed = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
                Product manual = newProduct("TIME-MANUAL");
                manual.setCreatedAt(fixed);
                manual.setUpdatedAt(fixed);
                mapper.insert(manual);
                session.commit();
                Product stored = mapper.selectById(manual.getProductId());
                Assert.assertEquals(fixed, stored.getCreatedAt());
                Assert.assertEquals(fixed, stored.getUpdatedAt());
                stored.setProductName("修改时间验证");
                Assert.assertEquals(1, mapper.updateById(stored));
                session.commit();
                stored = mapper.selectById(manual.getProductId());
                Assert.assertEquals(fixed, stored.getCreatedAt());
                Assert.assertTrue(stored.getUpdatedAt().isAfter(fixed));
                ConsoleOutput.info("创建时间保持=" + stored.getCreatedAt() + "，修改时间刷新=" + stored.getUpdatedAt());
            }
        }
    }

    /** MP 条件构造器：验证基础 Lambda 和项目全部联表筛选条件。 */
    @Test
    public void testConditionWrappers() throws Exception {
        // Lambda 条件查询：eq 表示相等，ge 表示大于等于，筛选在售且单价至少 100 的商品。
        {
            ConsoleOutput.operation("MP Lambda 筛选：在售且单价大于等于 100。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                        .eq(Product::getStatus, 1)
                        .ge(Product::getUnitPrice, new BigDecimal("100.00"));
                List<Product> products = ConsoleOutput.products("MP 条件查询结果",
                        session.getMapper(ProductPlusMapper.class).selectList(query));
                Assert.assertEquals(1, products.size());
                Assert.assertEquals("TEST-001", products.get(0).getProductCode());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 项目实际条件构造器：覆盖名称、分类/品牌编号及名称、状态、价格闭区间。
        {
            ConsoleOutput.operation("通过项目自定义条件入口组合全部字段，并验证价格边界与停售状态。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                ProductQuery q = new ProductQuery();
                q.setProductName("键盘");
                q.setCategoryId(1L);
                q.setBrandId(1L);
                q.setCategory("数码配件");
                q.setBrand("TestBrand");
                q.setStatus(1);
                q.setMinPrice(new BigDecimal("199.00"));
                q.setMaxPrice(new BigDecimal("199.00"));
                List<Product> rows = ConsoleOutput.products("完整条件", mapper.selectByCondition(q));
                Assert.assertEquals(1, rows.size());
                Assert.assertEquals("数码配件", rows.get(0).getCategory());
                Assert.assertEquals("TestBrand", rows.get(0).getBrand());
                q.setBrand("不存在的品牌");
                Assert.assertTrue(mapper.selectByCondition(q).isEmpty());
                q.setBrand("TestBrand");
                q.setCategory("办公用品");
                Assert.assertTrue(mapper.selectByCondition(q).isEmpty());
                q.setCategory("数码配件");
                q.setMaxPrice(new BigDecimal("198.99"));
                Assert.assertTrue(mapper.selectByCondition(q).isEmpty());
                Product stopped = newProduct("PLUS-STOPPED");
                stopped.setStatus(0);
                mapper.insert(stopped);
                ProductQuery onlyStopped = new ProductQuery();
                onlyStopped.setStatus(0);
                Assert.assertEquals(stopped.getProductId(), mapper.selectByCondition(onlyStopped).get(0).getProductId());
                Assert.assertEquals(4, mapper.selectByCondition(null).size());
                ProductQuery blank = new ProductQuery();
                blank.setProductName("  ");
                blank.setCategory("  ");
                blank.setBrand("  ");
                Assert.assertEquals(4, mapper.selectByCondition(blank).size());
            }
        }
    }

    /** MP 分页：验证总数、筛选、稳定排序、空页、非法参数及单页限制。 */
    @Test
    public void testPagination() throws Exception {
        // 分页：直接使用 3 条基准数据，每页 2 条，前两页分别返回 2 条和 1 条。
        {
            ConsoleOutput.operation("每页 2 条，分别查询第一页和第二页。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                IPage<Product> first = ConsoleOutput.page("第一页",
                        mapper.selectPageByCondition(new Page<Product>(1, 2), new ProductQuery()));
                IPage<Product> second = ConsoleOutput.page("第二页",
                        mapper.selectPageByCondition(new Page<Product>(2, 2), new ProductQuery()));
                Assert.assertEquals(3L, first.getTotal());
                Assert.assertEquals(2L, first.getPages());
                Assert.assertEquals(2, first.getRecords().size());
                Assert.assertEquals(1, second.getRecords().size());
                Assert.assertNotEquals(first.getRecords().get(0).getProductId(), second.getRecords().get(0).getProductId());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 筛选分页：验证同价编号排序、总数与筛选一致、越界页为空。
        {
            ConsoleOutput.operation("同价商品按编号排序，分页总数使用相同筛选条件。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                Product samePrice = newProduct("SAME-PRICE");
                samePrice.setUnitPrice(new BigDecimal("199.00"));
                mapper.insert(samePrice);
                session.commit();
                List<Product> sorted = ConsoleOutput.products("价格降序、编号升序", mapper.selectByCondition(null));
                Assert.assertEquals(Arrays.asList(1L, samePrice.getProductId(), 3L, 2L),
                        sorted.stream().map(Product::getProductId).collect(Collectors.toList()));
                ProductQuery q = new ProductQuery();
                q.setCategoryId(1L);
                IPage<Product> first = ConsoleOutput.page("筛选第一页", mapper.selectPageByCondition(new Page<Product>(1, 1), q));
                IPage<Product> second = ConsoleOutput.page("筛选第二页", mapper.selectPageByCondition(new Page<Product>(2, 1), q));
                Assert.assertEquals(2L, first.getTotal());
                Assert.assertEquals(2L, first.getPages());
                Assert.assertEquals(Long.valueOf(1), first.getRecords().get(0).getProductId());
                Assert.assertEquals(samePrice.getProductId(), second.getRecords().get(0).getProductId());
                IPage<Product> beyond = mapper.selectPageByCondition(new Page<Product>(3, 1), q);
                Assert.assertEquals(2L, beyond.getTotal());
                Assert.assertTrue(beyond.getRecords().isEmpty());
                q.setCategoryId(999L);
                IPage<Product> empty = mapper.selectPageByCondition(new Page<Product>(1, 2), q);
                Assert.assertEquals(0L, empty.getTotal());
                Assert.assertTrue(empty.getRecords().isEmpty());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 分页边界：拒绝非法页码/条数，插件限制每页最多 100 条。
        {
            ConsoleOutput.operation("验证分页入参及每页最多 100 条的插件限制。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.selectPageByCondition(null, null));
                Page<Product> invalidCurrent = new Page<>(1, 2);
                invalidCurrent.setCurrent(0);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.selectPageByCondition(invalidCurrent, null));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.selectPageByCondition(new Page<Product>(1, 0), null));
                IPage<Product> limited = ConsoleOutput.page("超过单页限制", mapper.selectPageByCondition(new Page<Product>(1, 101), null));
                Assert.assertEquals(100L, limited.getSize());
                Assert.assertEquals(3L, limited.getTotal());
            }
        }
    }

    /** MP 逻辑删除：通用查询、联查和分页隐藏删除记录，并拒绝更新已删除商品。 */
    @Test
    public void testLogicalDelete() throws Exception {
        // MP 逻辑删除：通用查询、自定义联查和分页均应隐藏已删除商品。
        {
            ConsoleOutput.operation("MP 删除后核对通用查询、联表查询和分页，并检查物理记录保留。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                Assert.assertEquals(1, ConsoleOutput.rows("MP 逻辑删除", mapper.deleteById(1L)));
                Assert.assertEquals(0, mapper.deleteById(1L));
                session.commit();
                Assert.assertNull(mapper.selectById(1L));
                Assert.assertEquals(2, mapper.selectList(null).size());
                Assert.assertEquals(2, ConsoleOutput.products("联查过滤已删除商品", mapper.selectByCondition(null)).size());
                Assert.assertEquals(2L, ConsoleOutput.page("分页过滤已删除商品",
                        mapper.selectPageByCondition(new Page<Product>(1, 10), null)).getTotal());
                try (java.sql.Statement statement = session.getConnection().createStatement();
                     java.sql.ResultSet row = statement.executeQuery("SELECT deleted FROM product WHERE product_id=1")) {
                    Assert.assertTrue(row.next());
                    Assert.assertEquals(1, row.getInt(1));
                }
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 已删除对象：即使版本有效也不能重新修改或恢复商品。
        {
            ConsoleOutput.operation("已删除商品的 MP 更新影响 0 行，不会恢复旧记录。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
                Product old = mapper.selectById(1L);
                Assert.assertEquals(1, mapper.deleteById(1L));
                old.setProductName("不应恢复的商品");
                Assert.assertEquals(0, ConsoleOutput.rows("更新已删除商品", mapper.updateById(old)));
                session.commit();
                Assert.assertNull(mapper.selectById(1L));
                Assert.assertEquals(2, mapper.selectByCondition(null).size());
            }
        }
    }

    /** 版本保护：缺少版本的 MP 和原生底层更新都不能绕过校验。 */
    @Test
    public void testMissingVersionGuard() throws Exception {
        ConsoleOutput.operation("缺失版本号时拒绝 MP 和原生底层更新。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Product changes = new Product();
            changes.setProductId(1L);
            changes.setProductName("缺版本不应写入");
            ProductPlusMapper plus = session.getMapper(ProductPlusMapper.class);
            assertMissingVersion(() -> plus.updateById(changes));
            assertMissingVersion(() -> plus.update(changes, new LambdaQueryWrapper<Product>().eq(Product::getProductId, 1L)));
            ProductMapper basic = session.getMapper(ProductMapper.class);
            assertMissingVersion(() -> basic.updateRow(changes));
            assertMissingVersion(() -> basic.updateDynamicFields(changes));
            Assert.assertEquals("测试键盘", basic.selectById(1L).getProductName());
        }
    }

    /** 分类品牌管理：验证通用 CRUD 及已被商品引用的记录受到外键保护。 */
    @Test
    public void testCategoryAndBrandCrud() throws Exception {
        ConsoleOutput.operation("新增、修改和删除独立分类品牌，检查引用中的记录不能删除。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            CategoryMapper categories = session.getMapper(CategoryMapper.class);
            BrandMapper brands = session.getMapper(BrandMapper.class);
            Category category = new Category();
            category.setCategoryName("临时分类");
            Brand brand = new Brand();
            brand.setBrandName("临时品牌");
            Assert.assertEquals(1, categories.insert(category));
            Assert.assertEquals(1, brands.insert(brand));
            Assert.assertNotNull(category.getId());
            Assert.assertNotNull(brand.getId());
            category.setCategoryName("修改后的分类");
            brand.setBrandName("修改后的品牌");
            Assert.assertEquals(1, categories.updateById(category));
            Assert.assertEquals(1, brands.updateById(brand));
            Assert.assertEquals("修改后的分类", categories.selectById(category.getId()).getCategoryName());
            Assert.assertEquals("修改后的品牌", brands.selectById(brand.getId()).getBrandName());
            Assert.assertEquals(1, categories.deleteById(category.getId()));
            Assert.assertEquals(1, brands.deleteById(brand.getId()));
            session.commit();
            Assert.assertNull(categories.selectById(category.getId()));
            Assert.assertNull(brands.selectById(brand.getId()));
            Assert.assertThrows(PersistenceException.class, () -> categories.deleteById(1L));
            session.rollback();
            Assert.assertThrows(PersistenceException.class, () -> brands.deleteById(1L));
            session.rollback();
            Assert.assertNotNull(categories.selectById(1L));
            Assert.assertNotNull(brands.selectById(1L));
            ConsoleOutput.info("未引用的分类品牌已删除；在用分类品牌受到外键保护。");
        }
    }

    /** 非测试辅助方法：MyBatis 可能包装异常，沿原因链确认实际是缺少版本。 */
    private static void assertMissingVersion(ThrowingRunnable action) {
        Throwable error = Assert.assertThrows(RuntimeException.class, action);
        while (!(error instanceof IllegalArgumentException) && error.getCause() != null) error = error.getCause();
        Assert.assertTrue("应因缺少版本号被拒绝", error instanceof IllegalArgumentException);
    }
}
