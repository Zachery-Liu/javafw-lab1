package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Basic product CRUD operations implemented by MyBatis XML.
 */
public interface ProductMapper {
    List<Product> selectAll();

    List<Product> selectByCondition(ProductQuery query);

    Product selectById(@Param("productId") Long productId);

    int insert(Product product);

    int update(Product product);

    /** Updates only non-null fields; null means the existing value is kept. */
    default int updateDynamic(Product product) {
        if (product == null || product.getProductId() == null) {
            throw new IllegalArgumentException("productId is required for dynamic update");
        }
        if (product.getProductCode() == null && product.getProductName() == null
                && product.getCategory() == null && product.getBrand() == null
                && product.getUnit() == null && product.getUnitPrice() == null
                && product.getStockQuantity() == null && product.getMinStock() == null
                && product.getStatus() == null && product.getProductionDate() == null
                && product.getDescription() == null) {
            throw new IllegalArgumentException("at least one field is required for dynamic update");
        }
        return updateDynamicFields(product);
    }

    int updateDynamicFields(Product product);

    int deleteById(@Param("productId") Long productId);
}
