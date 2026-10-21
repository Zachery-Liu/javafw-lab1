package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import cn.edu.tjut.huaxin.product.query.CategorySummary;
import cn.edu.tjut.huaxin.product.util.ProductBatchService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** 原生 MyBatis：基础 CRUD、动态 SQL、批量执行、统计和异常回滚。 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductMapperTest extends AbstractH2MapperTest {

    /** 按编号及集合查询：核对单条、IN、嵌套分组和空集合。 */
    @Test
    public void testProductQueries() throws Exception {
        // 查询：按 ID 找到商品，核对编码。
        {
            ConsoleOutput.operation("按编号 1 查询商品，核对商品编码。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Product product = ConsoleOutput.product("按 ID 查询", session.getMapper(ProductMapper.class).selectById(1L));
                Assert.assertNotNull(product);
                Assert.assertEquals("TEST-001", product.getProductCode());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // foreach：验证 IN、嵌套分类分组和空集合不误查全部数据。
        {
            ConsoleOutput.operation("验证编号集合、分类分组，以及 null/空集合分支。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductAdvancedMapper mapper = session.getMapper(ProductAdvancedMapper.class);
                assertIds("IN 查询", mapper.selectByIds(Arrays.asList(1L, 3L)), 1L, 3L);
                assertIds("空 IN", mapper.selectByIds(Collections.emptyList()));
                assertIds("null IN", mapper.selectByIds(null));
                Map<Long, List<Long>> groups = new LinkedHashMap<>();
                groups.put(1L, Arrays.asList(1L, 2L));
                groups.put(2L, Collections.emptyList());
                groups.put(3L, Arrays.asList(3L));
                groups.put(4L, null);
                assertIds("分类与编号同时匹配", mapper.selectByCategoryGroups(groups), 1L, 3L);
                assertIds("空分组", mapper.selectByCategoryGroups(Collections.emptyMap()));
                assertIds("null 分组", mapper.selectByCategoryGroups(null));
            }
        }
    }

    /** 新增商品：验证单条主键回填及多行批量新增持久化。 */
    @Test
    public void testInsertAndBatchInsert() throws Exception {
        // 新增：提交商品，检查影响行数和回填的主键，再按主键查回。
        {
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

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 多行 INSERT：核对影响行数和新会话中实际持久化的编码。
        {
            ConsoleOutput.operation("用一条多行 INSERT 新增两件商品并提交。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Assert.assertEquals(2, ConsoleOutput.rows("批量新增", session.getMapper(ProductMapper.class)
                        .insertBatch(Arrays.asList(newProduct("MULTI-001"), newProduct("MULTI-002")))));
                session.commit();
            }
            try (SqlSession session = sqlSessionFactory.openSession()) {
                List<Product> products = ConsoleOutput.products("批量新增后", session.getMapper(ProductMapper.class).selectAll());
                Assert.assertEquals(5, products.size());
                Assert.assertTrue(products.stream().anyMatch(p -> "MULTI-001".equals(p.getProductCode())));
                Assert.assertTrue(products.stream().anyMatch(p -> "MULTI-002".equals(p.getProductCode())));
            }
        }
    }

    /** 商品更新：验证动态字段、trim、版本递增及原生乐观锁冲突。 */
    @Test
    public void testUpdatesAndNativeOptimisticLock() throws Exception {
        // 动态修改：只传名称及查询得到的版本，其他业务字段保持原值。
        {
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

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // trim：验证去掉首个 AND、去掉 SET 尾逗号和旧版本拒绝更新。
        {
            ConsoleOutput.operation("运行 trim 查询与更新，核对未修改字段和版本冲突。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductAdvancedMapper mapper = session.getMapper(ProductAdvancedMapper.class);
                assertIds("trim 空筛选", mapper.selectWithTrim(new ProductQuery()), 1L, 2L, 3L);
                ProductQuery q = new ProductQuery();
                q.setProductName("键盘");
                q.setCategoryId(1L);
                q.setStatus(1);
                assertIds("trim 组合条件", mapper.selectWithTrim(q), 1L);
                Product changes = new Product();
                changes.setProductId(1L);
                changes.setVersion(0);
                changes.setDescription("trim 更新说明");
                Assert.assertEquals(1, ConsoleOutput.rows("trim 更新", mapper.updateWithTrim(changes)));
                Assert.assertEquals(0, ConsoleOutput.rows("trim 旧版本更新", mapper.updateWithTrim(changes)));
                Product stored = ConsoleOutput.product("trim 更新后", session.getMapper(ProductMapper.class).selectById(1L));
                Assert.assertEquals("测试键盘", stored.getProductName());
                Assert.assertEquals("trim 更新说明", stored.getDescription());
                Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 原生 XML 乐观锁：先固定更新，再拒绝旧版本动态更新和固定更新。
        {
            ConsoleOutput.operation("验证原生 XML 更新的版本递增和旧版本冲突。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                Product current = mapper.selectById(1L);
                Product stale = new Product();
                stale.setProductId(1L);
                stale.setVersion(0);
                stale.setProductName("旧版本名称");
                current.setProductName("原生固定更新名称");
                Assert.assertEquals(1, ConsoleOutput.rows("原生固定更新", mapper.update(current)));
                Assert.assertEquals(Integer.valueOf(1), current.getVersion());
                Assert.assertEquals(0, ConsoleOutput.rows("旧版本动态更新", mapper.updateDynamic(stale)));
                Assert.assertEquals(0, ConsoleOutput.rows("旧版本固定更新", mapper.update(stale)));
                session.commit();
                Assert.assertEquals("原生固定更新名称", ConsoleOutput.product("冲突后保存结果", mapper.selectById(1L)).getProductName());
            }
        }
    }

    /** 逻辑删除：验证单条删除、批量删除及未选中商品保留。 */
    @Test
    public void testLogicalDeleteAndBatchDelete() throws Exception {
        // 逻辑删除：设置 deleted 标记，之后普通查询返回 null。
        {
            ConsoleOutput.operation("逻辑删除编号 3，检查普通查询不可见。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                Assert.assertEquals(1, ConsoleOutput.rows("逻辑删除", mapper.deleteById(3L)));
                session.commit();
                ConsoleOutput.info("删除事务已提交。");
                Assert.assertNull(ConsoleOutput.product("删除后查询", mapper.selectById(3L)));
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 批量删除：foreach 把两个 ID 展开成 IN 参数，只删除选中的商品。
        {
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

    /** 条件查询：验证 if/where 全部筛选条件及 choose 优先级和默认分支。 */
    @Test
    public void testConditionalAndPriorityQueries() throws Exception {
        // 动态查询：if/where 组合名称和状态两个条件，找到在售键盘。
        {
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

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // if/where：覆盖分类、品牌、价格闭区间、空条件及删除过滤。
        {
            ConsoleOutput.operation("组合全部筛选字段，验证边界价格和删除过滤。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                ProductQuery q = new ProductQuery();
                q.setCategoryId(1L);
                q.setBrandId(1L);
                q.setCategory("数码配件");
                q.setBrand("TestBrand");
                q.setStatus(1);
                q.setMinPrice(new BigDecimal("199.00"));
                q.setMaxPrice(new BigDecimal("199.00"));
                assertIds("包含价格边界", mapper.selectByCondition(q), 1L);
                q.setMaxPrice(new BigDecimal("198.99"));
                assertIds("价格不匹配", mapper.selectByCondition(q));
                assertIds("空条件", mapper.selectByCondition(new ProductQuery()), 1L, 2L, 3L);
                mapper.deleteById(1L);
                assertIds("删除后空条件", mapper.selectByCondition(new ProductQuery()), 2L, 3L);
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // choose：依次验证名称、分类编号、分类名称、状态和默认分支。
        {
            ConsoleOutput.operation("依次验证 choose 优先级和默认在售条件。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                ProductQuery q = new ProductQuery();
                q.setProductName("键盘");
                q.setCategoryId(2L);
                q.setCategory("生活用品");
                q.setStatus(2);
                assertIds("名称优先", mapper.selectByPriority(q), 1L);
                q.setProductName(null);
                assertIds("分类编号优先", mapper.selectByPriority(q), 2L);
                q.setCategoryId(null);
                assertIds("分类名称优先", mapper.selectByPriority(q), 3L);
                q.setCategory(null);
                assertIds("状态分支", mapper.selectByPriority(q), 3L);
                q.setStatus(null);
                assertIds("默认在售", mapper.selectByPriority(q), 1L, 2L);
                Product stopped = newProduct("STOPPED-001");
                stopped.setStatus(0);
                mapper.insert(stopped);
                q.setStatus(0);
                assertIds("停售状态 0 有效", mapper.selectByPriority(q), stopped.getProductId());
            }
        }
    }

    /** BATCH 提交：分批执行并统一提交，最后不足一批也能保存。 */
    @Test
    public void testBatchExecutorCommit() throws Exception {
        ConsoleOutput.operation("BATCH 分批执行 5 件商品，每批 2 件，最后统一提交。");
        List<Product> products = new ArrayList<>();
        for (int i = 1; i <= 5; i++) products.add(newProduct("BATCH-" + i));
        Assert.assertEquals(5, ConsoleOutput.rows("BATCH 新增", ProductBatchService.insert(sqlSessionFactory, products, 2)));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Product> stored = session.getMapper(ProductMapper.class).selectAll();
            Assert.assertEquals(8, stored.size());
            for (Product product : products) {
                Assert.assertNotNull(product.getProductId());
                Assert.assertEquals(product.getProductCode(), ConsoleOutput.product("批次保存结果",
                        session.getMapper(ProductMapper.class).selectById(product.getProductId())).getProductCode());
            }
        }
    }

    /** 事务回滚：检查批次失败和显式回滚后的数据，并核对删除边界。 */
    @Test
    public void testTransactionRollback() throws Exception {
        // BATCH 回滚：第一批已 flush，第二批违反唯一约束，第一批也必须撤销。
        {
            ConsoleOutput.operation("第二批重复编码导致失败，检查第一批已执行的数据也被回滚。");
            Assert.assertThrows(PersistenceException.class, () -> ProductBatchService.insert(sqlSessionFactory,
                    Arrays.asList(newProduct("ROLLBACK-001"), newProduct("TEST-001")), 1));
            try (SqlSession session = sqlSessionFactory.openSession()) {
                List<Product> stored = ConsoleOutput.products("异常回滚后", session.getMapper(ProductMapper.class).selectAll());
                Assert.assertEquals(3, stored.size());
                Assert.assertFalse(stored.stream().anyMatch(p -> "ROLLBACK-001".equals(p.getProductCode())));
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 回滚和逻辑删除：检查实际行仍存在，重复删除及不存在编号返回 0。
        {
            ConsoleOutput.operation("显式回滚新增，并检查逻辑删除保留物理记录。");
            Long rolledBackId;
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Product product = newProduct("ROLLBACK-EXPLICIT");
                session.getMapper(ProductMapper.class).insert(product);
                rolledBackId = product.getProductId();
                session.rollback();
            }
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                Assert.assertNull(mapper.selectById(rolledBackId));
                Assert.assertNull(mapper.selectById(999L));
                Assert.assertEquals(0, mapper.deleteById(999L));
                Assert.assertEquals(1, mapper.deleteById(3L));
                Assert.assertEquals(0, mapper.deleteById(3L));
                session.commit();
                try (java.sql.Statement statement = session.getConnection().createStatement();
                     java.sql.ResultSet row = statement.executeQuery("SELECT deleted FROM product WHERE product_id=3")) {
                    Assert.assertTrue(row.next());
                    Assert.assertEquals(1, row.getInt(1));
                    ConsoleOutput.info("商品物理记录仍存在，deleted=" + row.getInt(1));
                }
            }
        }
    }

    /** 批量调价：价格分别更新，只允许版本匹配的商品写入。 */
    @Test
    public void testBatchPriceVersionConflicts() throws Exception {
        ConsoleOutput.operation("两件商品批量调价，一件版本有效，一件使用旧版本。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper basic = session.getMapper(ProductMapper.class);
            Product first = basic.selectById(1L);
            Product stale = basic.selectById(2L);
            first.setUnitPrice(new BigDecimal("210.00"));
            stale.setUnitPrice(new BigDecimal("20.00"));
            stale.setVersion(99);
            Assert.assertEquals(1, ConsoleOutput.rows("带版本批量调价",
                    session.getMapper(ProductAdvancedMapper.class).updatePrices(Arrays.asList(first, stale))));
            session.commit();
            Assert.assertEquals(0, new BigDecimal("210.00").compareTo(basic.selectById(1L).getUnitPrice()));
            Assert.assertEquals(Integer.valueOf(1), basic.selectById(1L).getVersion());
            Assert.assertEquals(0, new BigDecimal("15.50").compareTo(basic.selectById(2L).getUnitPrice()));
            Assert.assertEquals(Integer.valueOf(0), basic.selectById(2L).getVersion());
        }
    }

    /** 参数校验：拒绝缺少主键、版本、业务字段和无效批量参数。 */
    @Test
    public void testWriteArgumentValidation() throws Exception {
        // 无效参数：验证没有主键、版本、业务字段或有效批量集合时拒绝执行。
        {
            ConsoleOutput.operation("拒绝缺失主键、版本、更新字段和无效批量参数。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updateDynamic(null));
                Product changes = new Product();
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updateDynamic(changes));
                changes.setProductId(1L);
                changes.setProductName("不应写入");
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.update(changes));
                changes.setVersion(0);
                changes.setProductName(null);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updateDynamic(changes));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(null));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(Collections.emptyList()));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.insertBatch(Arrays.asList((Product) null)));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(null));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(Collections.emptyList()));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.deleteBatch(Arrays.asList(1L, null)));
                Assert.assertEquals("测试键盘", mapper.selectById(1L).getProductName());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 批量边界：无效批次、重复编号、缺失价格和缺失版本均不能写入。
        {
            ConsoleOutput.operation("验证 BATCH 参数和批量调价输入检查。");
            Assert.assertThrows(IllegalArgumentException.class, () -> ProductBatchService.insert(sqlSessionFactory, null, 2));
            Assert.assertThrows(IllegalArgumentException.class, () -> ProductBatchService.insert(sqlSessionFactory, Arrays.asList(newProduct("BAD-BATCH")), 0));
            Assert.assertThrows(IllegalArgumentException.class, () -> ProductBatchService.insert(sqlSessionFactory, Arrays.asList((Product) null), 1));
            Assert.assertEquals(0, ProductBatchService.insert(sqlSessionFactory, Collections.emptyList(), 2));
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductAdvancedMapper mapper = session.getMapper(ProductAdvancedMapper.class);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updatePrices(Collections.emptyList()));
                Product first = session.getMapper(ProductMapper.class).selectById(1L);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updatePrices(Arrays.asList(first, first)));
                first.setUnitPrice(null);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updatePrices(Arrays.asList(first)));
                first.setUnitPrice(new BigDecimal("-1.00"));
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updatePrices(Arrays.asList(first)));
                first.setVersion(null);
                Assert.assertThrows(IllegalArgumentException.class, () -> mapper.updatePrices(Arrays.asList(first)));
            }
        }
    }

    /** 分类统计：检查商品数、库存和金额，保留空分类并过滤已删除商品。 */
    @Test
    public void testCategorySummary() throws Exception {
        ConsoleOutput.operation("分类聚合核对数量、库存、金额，并保留没有可见商品的分类。");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            List<CategorySummary> rows = mapper.selectCategorySummary();
            Assert.assertEquals(4, rows.size());
            CategorySummary first = rows.get(0);
            Assert.assertEquals("数码配件", first.getCategoryName());
            Assert.assertEquals(Long.valueOf(1), first.getProductCount());
            Assert.assertEquals(Long.valueOf(20), first.getTotalStock());
            Assert.assertEquals(0, new BigDecimal("3980.00").compareTo(first.getInventoryValue()));
            Assert.assertEquals(Long.valueOf(0), rows.get(3).getProductCount());
            Assert.assertEquals(0, BigDecimal.ZERO.compareTo(rows.get(3).getInventoryValue()));
            mapper.deleteById(1L);
            rows = mapper.selectCategorySummary();
            Assert.assertEquals(4, rows.size());
            Assert.assertEquals(Long.valueOf(0), rows.get(0).getProductCount());
            Assert.assertEquals(Long.valueOf(0), rows.get(0).getTotalStock());
            for (CategorySummary row : rows) ConsoleOutput.info("分类=" + row.getCategoryName()
                    + "，商品数=" + row.getProductCount() + "，库存=" + row.getTotalStock()
                    + "，库存金额=" + row.getInventoryValue());
        }
    }

    /** 数据库约束：检查唯一、外键、非负价格库存及状态和删除标记取值。 */
    @Test
    public void testDatabaseConstraints() throws Exception {
        ConsoleOutput.operation("验证重复编码、无效外键、负数价格库存和非法状态被数据库拒绝。");
        Product duplicate = newProduct("TEST-001");
        assertRejectedInsert(duplicate);
        Product badCategory = newProduct("BAD-CATEGORY");
        badCategory.setCategoryId(999L);
        assertRejectedInsert(badCategory);
        Product badBrand = newProduct("BAD-BRAND");
        badBrand.setBrandId(999L);
        assertRejectedInsert(badBrand);
        Product badPrice = newProduct("BAD-PRICE");
        badPrice.setUnitPrice(new BigDecimal("-0.01"));
        assertRejectedInsert(badPrice);
        Product badStock = newProduct("BAD-STOCK");
        badStock.setStockQuantity(-1);
        assertRejectedInsert(badStock);
        Product badMinimum = newProduct("BAD-MINIMUM");
        badMinimum.setMinStock(-1);
        assertRejectedInsert(badMinimum);
        Product badStatus = newProduct("BAD-STATUS");
        badStatus.setStatus(3);
        assertRejectedInsert(badStatus);
        try (SqlSession session = sqlSessionFactory.openSession();
             java.sql.Statement statement = session.getConnection().createStatement()) {
            Assert.assertThrows(java.sql.SQLException.class, () -> statement.executeUpdate("UPDATE product SET deleted=2 WHERE product_id=1"));
            session.rollback();
        }
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Assert.assertEquals(3, session.getMapper(ProductMapper.class).selectAll().size());
        }
    }

    /** 非测试辅助方法：输出完整商品，并按顺序核对编号。 */
    private static void assertIds(String label, List<Product> products, Long... expected) {
        ConsoleOutput.products(label, products);
        Assert.assertEquals(label, Arrays.asList(expected), products.stream().map(Product::getProductId).collect(Collectors.toList()));
    }

    /** 非测试辅助方法：一次只验证一个数据库约束，失败事务立即回滚。 */
    private void assertRejectedInsert(Product product) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Assert.assertThrows(PersistenceException.class, () -> session.getMapper(ProductMapper.class).insert(product));
            session.rollback();
            ConsoleOutput.info("已拒绝无效商品并回滚，编码=" + product.getProductCode());
        }
    }
}
