package cn.edu.tjut.huaxin.product.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 商品实体：数据库 product 表的一条记录对应一个 Product 对象。 getter/setter 只读取或修改 Java 对象；保存到数据库仍需调用 Mapper。 */
@TableName("product")
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    // 主键由数据库自增；MP 插入成功后可将新编号回填到此属性。
    @TableId(value = "product_id", type = IdType.AUTO)
    private Long productId; // 数据库内部编号，不等于商品业务编码。

    private String productCode; // 商品业务编码，数据库唯一约束禁止重复。
    private String productName; // 商品名称。
    private Long categoryId; // 外键：product_category.id，分类必填。
    private Long brandId; // 外键：product_brand.id，品牌可为空。

    @TableField(exist = false)
    private String category; // 联表查询得到的分类名称，不写入 product 表。

    @TableField(exist = false)
    private String brand; // 联表查询得到的品牌名称，不写入 product 表。

    private String unit; // 计量单位，例如件、本、台。
    private BigDecimal unitPrice; // 金额使用精确十进制，对应数据库 DECIMAL。
    private Integer stockQuantity; // 当前库存；0 是有效值，不表示未提供。
    private Integer minStock; // 最低安全库存。
    private Integer status; // 业务状态：0 停售、1 在售、2 缺货。

    /** 独立删除标记：0 未删除、1 已删除；与商品在售状态无关。 */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;

    /** 乐观锁版本：更新时比较旧版本，成功后递增，冲突时影响行数为 0。 */
    @Version private Integer version;

    private LocalDate productionDate; // 生产日期，只包含年月日。

    @TableField(updateStrategy = FieldStrategy.NOT_NULL)
    private String description; // 商品说明。

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt; // MP 插入时填充，原生 SQL 仍可使用数据库默认值。

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt; // MP 修改时刷新；原生 SQL 由 MySQL ON UPDATE 维护。

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getMinStock() {
        return minStock;
    }

    public void setMinStock(Integer minStock) {
        this.minStock = minStock;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        // 将对象内容转为可读文字用于调试；不会查询或修改数据库。
        return "Product{"
                + "productId="
                + productId
                + ", productCode="
                + productCode
                + ", productName="
                + productName
                + ", categoryId="
                + categoryId
                + ", brandId="
                + brandId
                + ", category="
                + category
                + ", brand="
                + brand
                + ", unit="
                + unit
                + ", unitPrice="
                + unitPrice
                + ", stockQuantity="
                + stockQuantity
                + ", minStock="
                + minStock
                + ", status="
                + status
                + ", deleted="
                + deleted
                + ", version="
                + version
                + ", productionDate="
                + productionDate
                + ", description="
                + description
                + ", createdAt="
                + createdAt
                + ", updatedAt="
                + updatedAt
                + '}';
    }
}
