package cn.edu.tjut.huaxin.product.entity;

import java.util.List;

/** 教学关联结果：CategoryGraph，getter/setter 仅操作内存对象。 */
public class CategoryGraph extends Category {
    private List<Product> products;

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
