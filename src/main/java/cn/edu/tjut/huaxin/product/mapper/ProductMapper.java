package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Basic product CRUD operations implemented by MyBatis XML.
 */
public interface ProductMapper {
    List<Product> selectAll();

    Product selectById(@Param("productId") Long productId);

    int insert(Product product);

    int update(Product product);

    int deleteById(@Param("productId") Long productId);
}
