package cn.edu.tjut.huaxin.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 分类表：一个分类可以对应多个商品，名称唯一。 */
@TableName("product_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String categoryName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
