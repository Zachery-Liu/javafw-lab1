package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Category;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/** 分类通用 CRUD；被商品引用的分类由数据库外键禁止物理删除。 */
public interface CategoryMapper extends BaseMapper<Category> {
}
