package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;

import org.apache.ibatis.annotations.*;

import java.util.*;

/** 原生注解 CRUD；delete 用 @Update 保持业务逻辑删除，参数不拼接。 */
public interface ProductAnnotationMapper {
    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    @Results(
            id = "annotatedProduct",
            value = {@Result(id = true, column = "product_id", property = "productId")})
    Product selectById(@Param("id") Long id);

    @SelectProvider(type = ProductSqlProvider.class, method = "selectByCondition")
    @ResultMap("annotatedProduct")
    List<Product> selectByCondition(ProductQuery query);

    @Select(
            "SELECT * FROM product WHERE category_id=#{categoryId} AND status=#{status} AND"
                + " deleted=0 ORDER BY product_id")
    List<Product> selectByMap(Map<String, Object> values);

    @Select(
            "SELECT * FROM product WHERE category_id=#{categoryId} AND status=#{status} AND"
                + " deleted=0 ORDER BY product_id")
    List<Product> selectByParams(
            @Param("categoryId") Long categoryId, @Param("status") Integer status);

    @Insert(
            "INSERT INTO"
                + " product(product_code,product_name,category_id,brand_id,unit,unit_price,stock_quantity,min_stock,status)"
                + " VALUES(#{productCode},#{productName},#{categoryId},#{brandId},#{unit},#{unitPrice},#{stockQuantity},#{minStock},#{status})")
    @Options(useGeneratedKeys = true, keyProperty = "productId")
    int insert(Product product);

    @Update(
            "UPDATE product SET product_name=#{productName},version=version+1 WHERE"
                + " product_id=#{productId} AND version=#{version} AND deleted=0")
    int updateName(Product product);

    @Update("UPDATE product SET deleted=1 WHERE product_id=#{id} AND deleted=0")
    int deleteById(@Param("id") Long id);

    /** 同一个 Mapper：简单 SQL 用注解，联表 SQL 在 XML 中。 */
    Product selectJoinedById(@Param("productId") Long id);
}
