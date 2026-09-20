package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.h2.tools.RunScript;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ProductBatchAndPriorityTest {
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void buildSqlSessionFactory() throws Exception {
        try (InputStream input = Resources.getResourceAsStream("mybatis-config-test.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(input);
        }
    }

    @Before
    public void resetDatabase() throws Exception {
        try (SqlSession session = sqlSessionFactory.openSession();
             Connection connection = session.getConnection();
             Reader reader = Resources.getResourceAsReader("schema-h2.sql")) {
            RunScript.execute(connection, reader);
            connection.commit();
        }
    }

    @Test
    public void batchInsertAddsThreeProducts() {
        List<Product> products = Arrays.asList(newProduct("BATCH-1"),
                newProduct("BATCH-2"), newProduct("BATCH-3"));
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Assert.assertEquals(3, mapper.insertBatch(products));
            session.commit();

            List<Product> saved = mapper.selectAll();
            Assert.assertEquals(6, saved.size());
            for (Product product : products) {
                Assert.assertTrue(saved.stream().anyMatch(item ->
                        product.getProductCode().equals(item.getProductCode())));
            }
        }
    }

    @Test
    public void batchDeleteRemovesOnlyRequestedIds() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Assert.assertEquals(2, mapper.deleteBatch(Arrays.asList(1L, 3L)));
            session.commit();

            Assert.assertNull(mapper.selectById(1L));
            Assert.assertNotNull(mapper.selectById(2L));
            Assert.assertNull(mapper.selectById(3L));
        }
    }

    @Test
    public void batchOperationsRejectEmptyAndNullCollections() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mapper.insertBatch(Collections.emptyList()));
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mapper.insertBatch(null));
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mapper.insertBatch(Arrays.asList(newProduct("BATCH-1"), null)));
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mapper.deleteBatch(Collections.emptyList()));
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mapper.deleteBatch(null));
            Assert.assertThrows(IllegalArgumentException.class,
                    () -> mapper.deleteBatch(Arrays.asList(1L, null)));
            Assert.assertEquals(3, mapper.selectAll().size());
        }
    }

    @Test
    public void priorityQueryUsesNameBeforeCategoryAndStatus() {
        ProductQuery query = new ProductQuery();
        query.setProductName("键盘");
        query.setCategory("办公用品");
        query.setStatus(2);
        assertSingleProduct(query, "TEST-001");
    }

    @Test
    public void priorityQueryUsesCategoryBeforeStatus() {
        ProductQuery query = new ProductQuery();
        query.setCategory("办公用品");
        query.setStatus(2);
        assertSingleProduct(query, "TEST-002");
    }

    @Test
    public void priorityQueryUsesStatusWhenHigherFiltersAreAbsent() {
        ProductQuery query = new ProductQuery();
        query.setStatus(2);
        assertSingleProduct(query, "TEST-003");
    }

    @Test
    public void priorityQueryDefaultsToOnSaleProducts() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Product> products = session.getMapper(ProductMapper.class)
                    .selectByPriority(new ProductQuery());
            Assert.assertEquals(2, products.size());
            Assert.assertEquals("TEST-001", products.get(0).getProductCode());
            Assert.assertEquals("TEST-002", products.get(1).getProductCode());
        }
    }

    private void assertSingleProduct(ProductQuery query, String expectedCode) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Product> products = session.getMapper(ProductMapper.class).selectByPriority(query);
            Assert.assertEquals(1, products.size());
            Assert.assertEquals(expectedCode, products.get(0).getProductCode());
        }
    }

    private Product newProduct(String code) {
        Product product = new Product();
        product.setProductCode(code);
        product.setProductName("批量测试商品" + code);
        product.setCategory("测试分类");
        product.setBrand("TestBrand");
        product.setUnit("个");
        product.setUnitPrice(new BigDecimal("19.90"));
        product.setStockQuantity(10);
        product.setMinStock(2);
        product.setStatus(1);
        product.setProductionDate(LocalDate.of(2026, 6, 1));
        product.setDescription("批量测试数据");
        return product;
    }
}
