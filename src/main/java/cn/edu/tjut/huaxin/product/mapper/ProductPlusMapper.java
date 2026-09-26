package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

/**
 * Product CRUD operations provided by MyBatis-Plus without handwritten SQL.
 */
public interface ProductPlusMapper extends BaseMapper<Product> {
    default List<Product> selectByCondition(ProductQuery query) {
        return selectList(buildQueryWrapper(query));
    }

    default IPage<Product> selectPageByCondition(Page<Product> page, ProductQuery query) {
        if (page == null) {
            throw new IllegalArgumentException("page is required");
        }
        return selectPage(page, buildQueryWrapper(query));
    }

    static LambdaQueryWrapper<Product> buildQueryWrapper(ProductQuery query) {
        ProductQuery criteria = query == null ? new ProductQuery() : query;
        return new LambdaQueryWrapper<Product>()
                .like(hasText(criteria.getProductName()),
                        Product::getProductName, criteria.getProductName())
                .eq(hasText(criteria.getCategory()),
                        Product::getCategory, criteria.getCategory())
                .eq(hasText(criteria.getBrand()),
                        Product::getBrand, criteria.getBrand())
                .eq(criteria.getStatus() != null,
                        Product::getStatus, criteria.getStatus())
                .ge(criteria.getMinPrice() != null,
                        Product::getUnitPrice, criteria.getMinPrice())
                .le(criteria.getMaxPrice() != null,
                        Product::getUnitPrice, criteria.getMaxPrice())
                .orderByDesc(Product::getUnitPrice)
                .orderByAsc(Product::getProductId);
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
