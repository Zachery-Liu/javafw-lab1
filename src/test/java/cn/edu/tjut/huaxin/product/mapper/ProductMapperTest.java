package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
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
import java.util.List;

public class ProductMapperTest {
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void buildSqlSessionFactory() throws Exception {
        try (InputStream inputStream = Resources.getResourceAsStream("mybatis-config-test.xml")) {
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
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
    public void selectAllReturnsSeedProducts() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Product> products = session.getMapper(ProductMapper.class).selectAll();
            Assert.assertEquals(3, products.size());
        }
    }

    @Test
    public void selectByIdReturnsExpectedProduct() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Product product = session.getMapper(ProductMapper.class).selectById(1L);
            Assert.assertNotNull(product);
            Assert.assertEquals("TEST-001", product.getProductCode());
        }
    }

    @Test
    public void insertPersistsProductAndReturnsGeneratedId() {
        Product product = newProduct("TEST-NEW-004", "新增测试商品");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            int affectedRows = mapper.insert(product);
            session.commit();

            Assert.assertEquals(1, affectedRows);
            Assert.assertNotNull(product.getProductId());
            Assert.assertNotNull(mapper.selectById(product.getProductId()));
        }
    }

    @Test
    public void updateChangesStoredProduct() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            Product product = mapper.selectById(1L);
            product.setProductName("更新后的测试键盘");
            product.setUnitPrice(new BigDecimal("229.00"));

            int affectedRows = mapper.update(product);
            session.commit();

            Product updated = mapper.selectById(1L);
            Assert.assertEquals(1, affectedRows);
            Assert.assertEquals("更新后的测试键盘", updated.getProductName());
            Assert.assertEquals(0, new BigDecimal("229.00").compareTo(updated.getUnitPrice()));
        }
    }

    @Test
    public void deleteByIdRemovesProduct() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductMapper mapper = session.getMapper(ProductMapper.class);
            int affectedRows = mapper.deleteById(3L);
            session.commit();

            Assert.assertEquals(1, affectedRows);
            Assert.assertNull(mapper.selectById(3L));
        }
    }

    private Product newProduct(String productCode, String productName) {
        Product product = new Product();
        product.setProductCode(productCode);
        product.setProductName(productName);
        product.setCategory("测试分类");
        product.setBrand("TestBrand");
        product.setUnit("个");
        product.setUnitPrice(new BigDecimal("99.90"));
        product.setStockQuantity(10);
        product.setMinStock(2);
        product.setStatus(1);
        product.setProductionDate(LocalDate.of(2026, 6, 1));
        product.setDescription("JUnit新增测试数据");
        return product;
    }
}
