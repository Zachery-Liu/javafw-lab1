package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.*;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.FetchType;

/** 注解关联与 XML 对照：单对象 One，集合 Many；子查询使用全限定方法名。 */
public interface ProductRelationAnnotationMapper {
    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    @Results({
        @Result(id = true, column = "product_id", property = "productId"),
        @Result(
                column = "category_id",
                property = "categoryObject",
                one =
                        @One(
                                select =
                                        "cn.edu.tjut.huaxin.product.mapper.ProductRelationMapper.findCategory",
                                fetchType = FetchType.LAZY)),
        @Result(
                column = "brand_id",
                property = "brandObject",
                one =
                        @One(
                                select =
                                        "cn.edu.tjut.huaxin.product.mapper.ProductRelationMapper.findBrand",
                                fetchType = FetchType.LAZY)),
        @Result(
                column = "product_id",
                property = "detail",
                one =
                        @One(
                                select =
                                        "cn.edu.tjut.huaxin.product.mapper.ProductRelationMapper.findDetail",
                                fetchType = FetchType.LAZY)),
        @Result(
                column = "product_id",
                property = "tags",
                many =
                        @Many(
                                select =
                                        "cn.edu.tjut.huaxin.product.mapper.ProductRelationMapper.findTags",
                                fetchType = FetchType.LAZY))
    })
    ProductGraph selectGraph(Long id);

    @Select("SELECT * FROM product_category WHERE id=#{id}")
    @Results({
        @Result(id = true, column = "id", property = "id"),
        @Result(
                column = "id",
                property = "products",
                many =
                        @Many(
                                select =
                                        "cn.edu.tjut.huaxin.product.mapper.ProductRelationMapper.findCategoryProducts"))
    })
    CategoryGraph selectCategory(Long id);

    @Select("SELECT * FROM product_tag WHERE id=#{id}")
    @Results({
        @Result(id = true, column = "id", property = "id"),
        @Result(
                column = "id",
                property = "products",
                many =
                        @Many(
                                select =
                                        "cn.edu.tjut.huaxin.product.mapper.ProductRelationMapper.findTagProducts"))
    })
    TagGraph selectTag(Long id);
}
