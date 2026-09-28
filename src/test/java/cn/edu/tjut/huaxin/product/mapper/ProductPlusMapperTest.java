package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.session.SqlSession;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** MyBatis-Plus 基础测试：通用新增、Lambda 条件查询和分页，共 3 项。 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductPlusMapperTest extends AbstractH2MapperTest {
    /** BaseMapper 新增：不用手写 INSERT，检查主键回填和查询结果。 */
    @Test
    public void test01_insert() {
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

    /** Lambda 条件查询：eq 表示相等，ge 表示大于等于，筛选在售且单价至少 100 的商品。 */
    @Test
    public void test02_selectByLambda() {
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

    /** 分页：直接使用 3 条基准数据，每页 2 条，前两页分别返回 2 条和 1 条。 */
    @Test
    public void test03_selectPage() {
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
}
