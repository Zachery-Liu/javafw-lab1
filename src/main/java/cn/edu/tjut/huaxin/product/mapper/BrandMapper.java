package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Brand;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** 品牌通用 CRUD；被商品引用的品牌由数据库外键禁止物理删除。 */
public interface BrandMapper extends BaseMapper<Brand> {
}
