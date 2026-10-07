package cn.edu.tjut.huaxin.product.entity;

import java.util.List;

/** 教学关联结果：ProductGraph，getter/setter 仅操作内存对象。 */
public class ProductGraph extends Product {
    private Category categoryObject;
    private Brand brandObject;
    private ProductDetail detail;
    private List<Tag> tags;

    public Category getCategoryObject() {
        return categoryObject;
    }

    public void setCategoryObject(Category categoryObject) {
        this.categoryObject = categoryObject;
    }

    public Brand getBrandObject() {
        return brandObject;
    }

    public void setBrandObject(Brand brandObject) {
        this.brandObject = brandObject;
    }

    public ProductDetail getDetail() {
        return detail;
    }

    public void setDetail(ProductDetail detail) {
        this.detail = detail;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
