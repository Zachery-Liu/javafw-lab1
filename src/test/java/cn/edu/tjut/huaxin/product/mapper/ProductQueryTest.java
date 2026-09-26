package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import cn.edu.tjut.huaxin.product.util.MyBatisPlusFactoryBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.h2.tools.RunScript;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public class ProductQueryTest {
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void buildSqlSessionFactory() throws Exception {
        try (InputStream input = Resources.getResourceAsStream("mybatis-config-test.xml")) {
            sqlSessionFactory = MyBatisPlusFactoryBuilder.build(input);
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
    public void emptyQueryReturnsAllProducts() {
        List<Product> products = select(new ProductQuery());
        Assert.assertEquals(3, products.size());
        Assert.assertEquals("TEST-001", products.get(0).getProductCode());
    }

    @Test
    public void nameMatchesSubstring() {
        ProductQuery query = new ProductQuery();
        query.setProductName("键盘");

        List<Product> products = select(query);
        Assert.assertEquals(1, products.size());
        Assert.assertEquals("TEST-001", products.get(0).getProductCode());
    }

    @Test
    public void categoryBrandAndStatusAreCombined() {
        ProductQuery query = new ProductQuery();
        query.setCategory("办公用品");
        query.setBrand("TestBrand");
        query.setStatus(1);

        List<Product> products = select(query);
        Assert.assertEquals(1, products.size());
        Assert.assertEquals("TEST-002", products.get(0).getProductCode());

        query.setStatus(2);
        Assert.assertTrue(select(query).isEmpty());
    }

    @Test
    public void priceRangeIncludesBothBounds() {
        ProductQuery query = new ProductQuery();
        query.setMinPrice(new BigDecimal("15.50"));
        query.setMaxPrice(new BigDecimal("89.00"));

        List<Product> products = select(query);
        Assert.assertEquals(2, products.size());
        Assert.assertEquals("TEST-002", products.get(0).getProductCode());
        Assert.assertEquals("TEST-003", products.get(1).getProductCode());
    }

    @Test
    public void allFiltersCanBeCombined() {
        ProductQuery query = new ProductQuery();
        query.setProductName("测试");
        query.setCategory("数码配件");
        query.setBrand("TestBrand");
        query.setStatus(1);
        query.setMinPrice(new BigDecimal("199.00"));
        query.setMaxPrice(new BigDecimal("199.00"));

        List<Product> products = select(query);
        Assert.assertEquals(1, products.size());
        Assert.assertEquals("TEST-001", products.get(0).getProductCode());
    }

    private List<Product> select(ProductQuery query) {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            return session.getMapper(ProductMapper.class).selectByCondition(query);
        }
    }
}
