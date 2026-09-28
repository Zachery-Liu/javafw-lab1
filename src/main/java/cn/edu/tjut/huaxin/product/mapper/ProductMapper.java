package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import cn.edu.tjut.huaxin.product.query.CategorySummary;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 原生 MyBatis 操作接口，SQL 定义在 ProductMapper.xml 中。
 * 普通声明由框架代理执行；default 方法先做 Java 参数检查，再调用映射方法。
 * 所有普通查询过滤已删除商品，删除操作修改 deleted 标记并保留数据库记录。
 */
public interface ProductMapper {
    /** 查询全部未删除商品，按商品编号升序返回。 */
    List<Product> selectAll();

    /** 将有效条件用 AND 组合；空条件仍只返回未删除商品。 */
    List<Product> selectByCondition(ProductQuery query);

    /** 首个有效条件：名称、分类编号、分类名称、状态；默认查询在售商品。 */
    List<Product> selectByPriority(ProductQuery query);

    /** 参数名与 XML 的 #{productId} 对应；未找到或已删除时返回 null。 */
    Product selectById(@Param("productId") Long productId);

    /** 分类 LEFT JOIN 商品聚合：保留空分类，排除逻辑删除商品，统计库存金额。 */
    List<CategorySummary> selectCategorySummary();

    /** 返回影响行数；自增主键回填到 product.productId，而不是作为返回值。 */
    int insert(Product product);

    /** 检查集合后执行一条多行 INSERT；当前没有配置批量主键回填。 */
    default int insertBatch(List<Product> products) {
        // || 从左到右短路判断，先排除 null，避免调用空引用的集合方法。
        if (products == null || products.isEmpty() || products.contains(null)) {
            throw new IllegalArgumentException("products must contain at least one non-null product");
        }
        return insertBatchRows(products);
    }

    /** 底层映射入口；调用方应优先使用上面的 insertBatch，避免绕过校验。 */
    int insertBatchRows(@Param("products") List<Product> products);

    /** 固定更新业务字段；通常先查完整对象再修改，避免遗漏字段被写为 null。 */
    default int update(Product product) {
        requireVersion(product);
        int rows = updateRow(product);
        if (rows == 1) {
            product.setVersion(product.getVersion() + 1);
        }
        return rows;
    }

    int updateRow(Product product);

    /** 修改必须携带查询时读取的版本；版本冲突返回 0，由调用方提示刷新重试。 */
    static void requireVersion(Product product) {
        if (product == null || product.getProductId() == null || product.getVersion() == null) {
            throw new IllegalArgumentException("productId and version are required for update");
        }
    }

    /** 只修改非 null 字段；null 表示保留原值，因此当前接口不能表达显式置空。 */
    default int updateDynamic(Product product) {
        // 必须明确修改哪件商品，不能接受缺少主键的更新请求。
        if (product == null || product.getProductId() == null) {
            throw new IllegalArgumentException("productId is required for dynamic update");
        }
        requireVersion(product);
        // 11 个业务字段全部未提供时拒绝，避免 XML 生成没有 SET 内容的 UPDATE。
        if (product.getProductCode() == null && product.getProductName() == null
                && product.getCategoryId() == null && product.getBrandId() == null
                && product.getUnit() == null && product.getUnitPrice() == null
                && product.getStockQuantity() == null && product.getMinStock() == null
                && product.getStatus() == null && product.getProductionDate() == null
                && product.getDescription() == null) {
            throw new IllegalArgumentException("at least one field is required for dynamic update");
        }
        int rows = updateDynamicFields(product);
        if (rows == 1) {
            product.setVersion(product.getVersion() + 1);
        }
        return rows;
    }

    /** 底层动态更新映射，SQL 同时限定 deleted=0，禁止修改已删除商品。 */
    int updateDynamicFields(Product product);

    /** 逻辑删除一件商品；成功返回 1，重复删除或编号不存在通常返回 0。 */
    int deleteById(@Param("productId") Long productId);

    /** 校验 ID 集合，防止生成非法 IN 条件，再批量标记 deleted=1。 */
    default int deleteBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.contains(null)) {
            throw new IllegalArgumentException("ids must contain at least one non-null id");
        }
        return deleteBatchRows(ids);
    }

    /** XML foreach 使用 ids 这个参数名展开集合；优先从 deleteBatch 调用。 */
    int deleteBatchRows(@Param("ids") List<Long> ids);
}
