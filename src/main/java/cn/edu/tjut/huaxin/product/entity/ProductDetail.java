package cn.edu.tjut.huaxin.product.entity;

/** 教学关联结果：ProductDetail，getter/setter 仅操作内存对象。 */
public class ProductDetail {
    private Long productId;
    private String origin;
    private Integer warrantyMonths;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }
}
