package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** MyBatis-Plus 商品接口：继承 BaseMapper 获得框架生成的通用 CRUD。 通用操作读取 Product 的表名、主键和 @TableLogic，自动处理逻辑删除。 */
public interface ProductPlusMapper extends BaseMapper<Product> {
    /** 先将筛选参数转成 Wrapper，再调用通用列表查询。 */
    default List<Product> selectByCondition(ProductQuery query) {
        return selectJoinedList(buildQueryWrapper(query));
    }

    /** Page 携带页码、每页条数；查询后返回总数、总页数及当前页商品。 */
    default IPage<Product> selectPageByCondition(Page<Product> page, ProductQuery query) {
        if (page == null || page.getCurrent() < 1 || page.getSize() < 1) {
            throw new IllegalArgumentException("positive page and size are required");
        }
        return selectJoinedPage(page, buildQueryWrapper(query));
    }

    /** 自定义 SQL 联表补充分类、品牌名称；BaseMapper 通用查询只返回商品表字段。 */
    List<Product> selectJoinedList(@Param(Constants.WRAPPER) Wrapper<Product> wrapper);

    IPage<Product> selectJoinedPage(
            Page<Product> page, @Param(Constants.WRAPPER) Wrapper<Product> wrapper);

    /** 每次创建新条件容器，避免复用上一次查询的条件。 */
    static LambdaQueryWrapper<Product> buildQueryWrapper(ProductQuery query) {
        // null 查询对象转换为空条件；框架仍会添加 deleted=0，不会显示删除记录。
        ProductQuery criteria = query == null ? new ProductQuery() : query;
        // 条件方法的布尔参数决定是否加入 SQL；方法引用指定实体属性。
        // like 模糊匹配，eq 等于，ge/le 为价格闭区间；各条件默认 AND 连接。
        // 价格降序后再按编号升序，明确同价商品的顺序，便于分页。
        return new LambdaQueryWrapper<Product>()
                // 自定义联表 SQL 不会自动注入逻辑删除条件，需要明确过滤。
                .eq(Product::getDeleted, 0)
                .like(
                        hasText(criteria.getProductName()),
                        Product::getProductName,
                        criteria.getProductName())
                .eq(
                        criteria.getCategoryId() != null,
                        Product::getCategoryId,
                        criteria.getCategoryId())
                .eq(criteria.getBrandId() != null, Product::getBrandId, criteria.getBrandId())
                // {0} 由 MP 转换为绑定参数，名称不会直接拼接进 SQL。
                .apply(
                        hasText(criteria.getCategory()),
                        "category_id IN (SELECT id FROM product_category WHERE category_name ="
                            + " {0})",
                        criteria.getCategory())
                .apply(
                        hasText(criteria.getBrand()),
                        "brand_id IN (SELECT id FROM product_brand WHERE brand_name = {0})",
                        criteria.getBrand())
                .eq(criteria.getStatus() != null, Product::getStatus, criteria.getStatus())
                .ge(criteria.getMinPrice() != null, Product::getUnitPrice, criteria.getMinPrice())
                .le(criteria.getMaxPrice() != null, Product::getUnitPrice, criteria.getMaxPrice())
                .orderByDesc(Product::getUnitPrice)
                .orderByAsc(Product::getProductId);
    }

    /** 排除 null 与纯空白输入；这里仅判断，不会裁掉实际查询值两端的空格。 */
    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
