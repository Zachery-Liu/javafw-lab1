package cn.edu.tjut.huaxin.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 品牌表：一个品牌可以对应多个商品，名称唯一。 */
@TableName("product_brand")
public class Brand {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String brandName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
}
