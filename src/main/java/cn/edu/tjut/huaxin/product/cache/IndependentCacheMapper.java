package cn.edu.tjut.huaxin.product.cache;

import cn.edu.tjut.huaxin.product.entity.Product;

import org.apache.ibatis.annotations.*;

/** 第二个教学 namespace：对比隔离性及跨 namespace 旧缓存。 */
@CacheNamespace(size = 128, readWrite = true)
public interface IndependentCacheMapper {
    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    Product selectById(Long id);
}
