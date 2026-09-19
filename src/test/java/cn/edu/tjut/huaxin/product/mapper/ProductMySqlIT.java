package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import cn.edu.tjut.huaxin.product.util.MyBatisUtil;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Manual integration test against the local MySQL database.
 *
 * <p>The class name intentionally ends with IT, so the normal Maven test run
 * does not connect to MySQL. Run it explicitly after configuring the ignored
 * db.properties file.</p>
 */
public class ProductMySqlIT {

    @Test
    public void conditionalQueryAgainstMySql() {
        List<Product> existingProducts = selectAll();
        Assert.assertFalse("MySQL should contain sample products", existingProducts.isEmpty());

        Product expected = existingProducts.get(0);
        ProductQuery query = new ProductQuery();
        query.setProductName(expected.getProductName());
        query.setCategory(expected.getCategory());
        query.setBrand(expected.getBrand());
        query.setStatus(expected.getStatus());
        query.setMinPrice(expected.getUnitPrice());
        query.setMaxPrice(expected.getUnitPrice());

        try (SqlSession session = MyBatisUtil.openSession()) {
            List<Product> matches = session.getMapper(ProductMapper.class).selectByCondition(query);
            Assert.assertTrue(matches.stream().anyMatch(product ->
                    expected.getProductId().equals(product.getProductId())));
        }
    }

    @Test
    public void dynamicUpdateAgainstMySql() {
        Long productId = null;
        try {
            Product product = buildTemporaryProduct();
            productId = insert(product);

            Product changes = new Product();
            changes.setProductId(productId);
            changes.setProductName("MySQL动态更新商品");
            changes.setUnitPrice(new BigDecimal("149.90"));

            try (SqlSession session = MyBatisUtil.openSession()) {
                Assert.assertEquals(1, session.getMapper(ProductMapper.class).updateDynamic(changes));
                session.commit();
            }

            Product updated = selectById(productId);
            Assert.assertEquals("MySQL动态更新商品", updated.getProductName());
            Assert.assertEquals(0, new BigDecimal("149.90").compareTo(updated.getUnitPrice()));
            Assert.assertEquals(product.getCategory(), updated.getCategory());
            Assert.assertEquals(product.getBrand(), updated.getBrand());
            Assert.assertEquals(product.getStockQuantity(), updated.getStockQuantity());
        } finally {
            cleanup(productId);
        }
    }

    @Test
    public void crudAgainstMySql() {
        Long productId = null;

        try {
            List<Product> existingProducts = selectAll();
            Assert.assertNotNull(existingProducts);
            System.out.println("MySQL initial product count = " + existingProducts.size());

            Product product = buildTemporaryProduct();
            productId = insert(product);
            Assert.assertNotNull(productId);

            Product inserted = selectById(productId);
            Assert.assertNotNull(inserted);
            Assert.assertEquals(product.getProductCode(), inserted.getProductCode());

            inserted.setProductName("修改后的MySQL联调商品");
            inserted.setUnitPrice(new BigDecimal("129.90"));
            Assert.assertEquals(1, update(inserted));

            Product updated = selectById(productId);
            Assert.assertNotNull(updated);
            Assert.assertEquals("修改后的MySQL联调商品", updated.getProductName());
            Assert.assertEquals(
                    0,
                    new BigDecimal("129.90").compareTo(updated.getUnitPrice())
            );

            Assert.assertEquals(1, deleteById(productId));
            Assert.assertNull(selectById(productId));
            productId = null;
        } finally {
            cleanup(productId);
        }
    }

    private List<Product> selectAll() {
        try (SqlSession session = MyBatisUtil.openSession()) {
            return session.getMapper(ProductMapper.class).selectAll();
        }
    }

    private Product selectById(Long productId) {
        try (SqlSession session = MyBatisUtil.openSession()) {
            return session.getMapper(ProductMapper.class).selectById(productId);
        }
    }

    private Long insert(Product product) {
        try (SqlSession session = MyBatisUtil.openSession()) {
            int affectedRows = session.getMapper(ProductMapper.class).insert(product);
            session.commit();
            Assert.assertEquals(1, affectedRows);
            return product.getProductId();
        }
    }

    private int update(Product product) {
        try (SqlSession session = MyBatisUtil.openSession()) {
            int affectedRows = session.getMapper(ProductMapper.class).update(product);
            session.commit();
            return affectedRows;
        }
    }

    private int deleteById(Long productId) {
        try (SqlSession session = MyBatisUtil.openSession()) {
            int affectedRows = session.getMapper(ProductMapper.class).deleteById(productId);
            session.commit();
            return affectedRows;
        }
    }

    private void cleanup(Long productId) {
        if (productId == null) {
            return;
        }

        try (SqlSession session = MyBatisUtil.openSession()) {
            session.getMapper(ProductMapper.class).deleteById(productId);
            session.commit();
        }
    }

    private Product buildTemporaryProduct() {
        Product product = new Product();
        product.setProductCode("MYSQL-" + System.currentTimeMillis());
        product.setProductName("MySQL联调测试商品");
        product.setCategory("测试分类");
        product.setBrand("TestBrand");
        product.setUnit("个");
        product.setUnitPrice(new BigDecimal("99.90"));
        product.setStockQuantity(10);
        product.setMinStock(2);
        product.setStatus(1);
        product.setProductionDate(LocalDate.now());
        product.setDescription("真实MySQL CRUD联调临时数据");
        return product;
    }
}
