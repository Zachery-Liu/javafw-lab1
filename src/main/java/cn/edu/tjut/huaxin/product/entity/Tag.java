package cn.edu.tjut.huaxin.product.entity;

/** 教学关联结果：Tag，getter/setter 仅操作内存对象。 */
public class Tag {
    private Long id;
    private String tagName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }
}
