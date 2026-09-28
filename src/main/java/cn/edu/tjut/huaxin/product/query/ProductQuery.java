package cn.edu.tjut.huaxin.product.query;

import java.math.BigDecimal;

/** 商品查询条件，不对应数据库表；未提供的条件通常为 null，表示不参与筛选。 */
public class ProductQuery {
    private String productName; // 名称包含关键词。
    private Long categoryId; // 分类编号筛选，可与名称条件同时使用。
    private Long brandId; // 品牌编号筛选。
    private String category; // 分类等值筛选。
    private String brand; // 品牌等值筛选。
    private Integer status; // 状态等值筛选，0 也是合法条件。
    private BigDecimal minPrice; // 最低价格，包含边界。
    private BigDecimal maxPrice; // 最高价格，包含边界。

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getBrandId() { return brandId; }
    public void setBrandId(Long brandId) { this.brandId = brandId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
}
