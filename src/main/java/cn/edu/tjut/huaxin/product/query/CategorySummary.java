package cn.edu.tjut.huaxin.product.query;

import java.math.BigDecimal;

/** 分类与商品联表统计结果，不对应独立数据库表。 */
public class CategorySummary {
    private Long categoryId;
    private String categoryName;
    private Long productCount;
    private Long totalStock;
    private BigDecimal inventoryValue;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getProductCount() { return productCount; }
    public void setProductCount(Long productCount) { this.productCount = productCount; }
    public Long getTotalStock() { return totalStock; }
    public void setTotalStock(Long totalStock) { this.totalStock = totalStock; }
    public BigDecimal getInventoryValue() { return inventoryValue; }
    public void setInventoryValue(BigDecimal inventoryValue) { this.inventoryValue = inventoryValue; }
}
