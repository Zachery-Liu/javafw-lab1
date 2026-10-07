package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.*;

import org.apache.ibatis.annotations.Param;

import java.util.List;

/** XML 关联映射：JOIN 嵌套结果、嵌套 select、集合归并与多对多。 */
public interface ProductRelationMapper {
    ProductGraph selectGraph(@Param("id") Long id);

    ProductGraph selectLazyGraph(@Param("id") Long id);

    CategoryGraph selectCategory(@Param("id") Long id);

    TagGraph selectTag(@Param("id") Long id);

    Category findCategory(Long id);

    Brand findBrand(Long id);

    ProductDetail findDetail(Long id);

    List<Tag> findTags(Long id);

    List<Product> findCategoryProducts(Long id);

    List<Product> findTagProducts(Long id);
}
