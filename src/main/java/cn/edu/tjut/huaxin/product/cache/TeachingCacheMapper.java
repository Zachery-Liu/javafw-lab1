package cn.edu.tjut.huaxin.product.cache;

import cn.edu.tjut.huaxin.product.entity.Product;

import org.apache.ibatis.annotations.*;

/** 缓存专用教学 namespace，仅注册到独立缓存配置。 */
@CacheNamespace(size = 128, readWrite = true)
public interface TeachingCacheMapper {
    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    Product selectById(Long id);

    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    Product selectSameSql(Long id);

    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    @Options(useCache = false)
    Product selectUncached(Long id);

    @Select("SELECT * FROM product WHERE product_id=#{id} AND deleted=0")
    @Options(flushCache = Options.FlushCachePolicy.TRUE)
    Product selectAndFlush(Long id);

    @Update(
            "UPDATE product SET product_name=#{productName},version=version+1 WHERE"
                + " product_id=#{productId} AND version=#{version} AND deleted=0")
    int updateName(Product product);
}
