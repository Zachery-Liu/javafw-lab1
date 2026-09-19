package cn.edu.tjut.huaxin.product.query;

import java.math.BigDecimal;

/** Optional filters for product searches. */
public class ProductQuery {
    private String productName;
    private String category;
    private String brand;
    private Integer status;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
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
