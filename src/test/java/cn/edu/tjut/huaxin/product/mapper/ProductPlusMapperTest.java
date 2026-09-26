package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import cn.edu.tjut.huaxin.product.util.MyBatisPlusFactoryBuilder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.plugin.Interceptor;
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
import java.time.LocalDate;
import java.util.List;

public class ProductPlusMapperTest {
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
    public void insertReturnsGeneratedId() {
        Product product = newProduct("PLUS-004", "MyBatis-Plus Insert Product");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
            Assert.assertEquals(1, mapper.insert(product));
            session.commit();

            Assert.assertNotNull(product.getProductId());
            Assert.assertNotNull(mapper.selectById(product.getProductId()));
        }
    }

    @Test
    public void selectByIdReturnsExpectedProduct() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            Product product = session.getMapper(ProductPlusMapper.class).selectById(1L);
            Assert.assertNotNull(product);
            Assert.assertEquals("TEST-001", product.getProductCode());
        }
    }

    @Test
    public void selectListReturnsAllProducts() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Product> products = session.getMapper(ProductPlusMapper.class).selectList(null);
            Assert.assertEquals(3, products.size());
        }
    }

    @Test
    public void updateByIdChangesOnlyProvidedFields() {
        Product changes = new Product();
        changes.setProductId(1L);
        changes.setProductName("MyBatis-Plus Updated Product");
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
            Assert.assertEquals(1, mapper.updateById(changes));
            session.commit();

            Product updated = mapper.selectById(1L);
            Assert.assertEquals("MyBatis-Plus Updated Product", updated.getProductName());
            Assert.assertEquals("TEST-001", updated.getProductCode());
        }
    }

    @Test
    public void deleteByIdRemovesProduct() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
            Assert.assertEquals(1, mapper.deleteById(3L));
            session.commit();

            Assert.assertNull(mapper.selectById(3L));
        }
    }

    @Test
    public void lambdaQueryCombinesAllFilters() {
        ProductQuery query = new ProductQuery();
        query.setProductName("keyboard");
        query.setCategory("digital");
        query.setBrand("TestBrand");
        query.setStatus(1);
        query.setMinPrice(new BigDecimal("199.00"));
        query.setMaxPrice(new BigDecimal("199.00"));

        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
            Product product = mapper.selectById(1L);
            query.setProductName(product.getProductName());
            query.setCategory(product.getCategory());

            List<Product> products = mapper.selectByCondition(query);
            Assert.assertEquals(1, products.size());
            Assert.assertEquals("TEST-001", products.get(0).getProductCode());
        }
    }

    @Test
    public void lambdaQueryFiltersPriceAndSortsDescending() {
        ProductQuery query = new ProductQuery();
        query.setMinPrice(new BigDecimal("15.50"));
        query.setMaxPrice(new BigDecimal("89.00"));

        try (SqlSession session = sqlSessionFactory.openSession()) {
            List<Product> products = session.getMapper(ProductPlusMapper.class)
                    .selectByCondition(query);
            Assert.assertEquals(2, products.size());
            Assert.assertEquals("TEST-003", products.get(0).getProductCode());
            Assert.assertEquals("TEST-002", products.get(1).getProductCode());
        }
    }

    @Test
    public void paginationReturnsFirstAndSecondPagesOfThree() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            ProductPlusMapper mapper = session.getMapper(ProductPlusMapper.class);
            for (int index = 4; index <= 7; index++) {
                Product product = newProduct(
                        "PAGE-00" + index, "Page Product " + index);
                product.setUnitPrice(new BigDecimal(index * 10 + ".00"));
                Assert.assertEquals(1, mapper.insert(product));
            }

            IPage<Product> first = mapper.selectPageByCondition(
                    new Page<Product>(1, 3), new ProductQuery());
            IPage<Product> second = mapper.selectPageByCondition(
                    new Page<Product>(2, 3), new ProductQuery());

            Assert.assertEquals(1L, first.getCurrent());
            Assert.assertEquals(3L, first.getSize());
            Assert.assertEquals(7L, first.getTotal());
            Assert.assertEquals(3L, first.getPages());
            Assert.assertEquals(3, first.getRecords().size());

            Assert.assertEquals(2L, second.getCurrent());
            Assert.assertEquals(3L, second.getSize());
            Assert.assertEquals(7L, second.getTotal());
            Assert.assertEquals(3L, second.getPages());
            Assert.assertEquals(3, second.getRecords().size());
            Assert.assertTrue(first.getRecords().get(0).getUnitPrice()
                    .compareTo(second.getRecords().get(0).getUnitPrice()) > 0);
        }
    }

    @Test
    public void paginationAcceptsStatusCondition() {
        ProductQuery query = new ProductQuery();
        query.setStatus(1);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            IPage<Product> page = session.getMapper(ProductPlusMapper.class)
                    .selectPageByCondition(new Page<Product>(1, 1), query);

            Assert.assertEquals(2L, page.getTotal());
            Assert.assertEquals(2L, page.getPages());
            Assert.assertEquals(1, page.getRecords().size());
            Assert.assertEquals(Integer.valueOf(1),
                    page.getRecords().get(0).getStatus());
        }
    }

    @Test
    public void actualFactoryContainsPaginationInterceptor() {
        boolean found = false;
        for (Interceptor plugin :
                sqlSessionFactory.getConfiguration().getInterceptors()) {
            if (plugin instanceof MybatisPlusInterceptor) {
                MybatisPlusInterceptor plusPlugin =
                        (MybatisPlusInterceptor) plugin;
                found = plusPlugin.getInterceptors().stream().anyMatch(
                        item -> item instanceof PaginationInnerInterceptor);
            }
        }
        Assert.assertTrue("Pagination plugin must be registered", found);
    }

    private Product newProduct(String code, String name) {
        Product product = new Product();
        product.setProductCode(code);
        product.setProductName(name);
        product.setCategory("Test Category");
        product.setBrand("TestBrand");
        product.setUnit("item");
        product.setUnitPrice(new BigDecimal("109.90"));
        product.setStockQuantity(12);
        product.setMinStock(3);
        product.setStatus(1);
        product.setProductionDate(LocalDate.of(2026, 9, 21));
        product.setDescription("MyBatis-Plus BaseMapper test data");
        return product;
    }
}
