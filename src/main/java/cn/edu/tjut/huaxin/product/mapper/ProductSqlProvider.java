package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.query.ProductQuery;

import org.apache.ibatis.jdbc.SQL;

/** Provider 只选择 SQL 结构，参数值保持 #{}，不把输入拼进 SQL。 */
public final class ProductSqlProvider {
    /** 由 MyBatis 调用：根据有效属性添加固定条件，所有值通过占位符绑定。 */
    public String selectByCondition(ProductQuery query) {
        SQL sql = new SQL().SELECT("*").FROM("product").WHERE("deleted = 0");
        if (query != null) {
            if (query.getProductName() != null)
                sql.WHERE("product_name LIKE CONCAT('%', #{productName}, '%')");
            if (query.getCategoryId() != null) sql.WHERE("category_id = #{categoryId}");
            if (query.getStatus() != null) sql.WHERE("status = #{status}");
        }
        return sql.ORDER_BY("product_id").toString();
    }
}
