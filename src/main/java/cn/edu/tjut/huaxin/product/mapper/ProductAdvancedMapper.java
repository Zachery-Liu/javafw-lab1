package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;

import org.apache.ibatis.annotations.Param;

import java.util.*;

/** 动态 SQL 进阶：trim、IN、嵌套 foreach 与带版本校验的 CASE WHEN 更新。 */
public interface ProductAdvancedMapper {
    List<Product> selectWithTrim(ProductQuery query);

    List<Product> selectByIds(@Param("ids") List<Long> ids);

    List<Product> selectByCategoryGroups(@Param("groups") Map<Long, List<Long>> groups);

    int updateWithTrim(Product product);

    int updatePricesRows(@Param("products") List<Product> products);

    /** 检查集合、重复编号及版本后执行一条差异化更新；冲突项不写入。 */
    default int updatePrices(List<Product> products) {
        if (products == null || products.isEmpty())
            throw new IllegalArgumentException("products required");
        Set<Long> seen = new HashSet<>();
        for (Product product : products) {
            ProductMapper.requireVersion(product);
            if (product.getUnitPrice() == null
                    || product.getUnitPrice().signum() < 0
                    || !seen.add(product.getProductId()))
                throw new IllegalArgumentException("valid unique products and prices required");
        }
        return updatePricesRows(products);
    }
}
