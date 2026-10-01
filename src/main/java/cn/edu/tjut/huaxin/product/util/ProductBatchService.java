package cn.edu.tjut.huaxin.product.util;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.mapper.ProductMapper;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.session.*;

import java.sql.Statement;
import java.util.List;

/** BATCH 执行器演示：分批 flush 控制内存，全部成功后只提交一次。 */
public final class ProductBatchService {
    /** 非测试业务方法：批次大小控制 flush 频率，最后统一提交；任何异常都回滚。 */
    public static int insert(SqlSessionFactory factory, List<Product> products, int batchSize) {
        if (products == null || batchSize <= 0 || products.contains(null))
            throw new IllegalArgumentException("invalid batch");
        try (SqlSession session = factory.openSession(ExecutorType.BATCH)) {
            int count = 0;
            for (Product product : products) {
                session.getMapper(ProductMapper.class).insert(product);
                if (++count % batchSize == 0) verify(session.flushStatements());
            }
            verify(session.flushStatements());
            session.commit();
            return count;
        }
        // 异常时 try-with-resources 关闭未提交事务并回滚，已 flush 不代表已 commit。
    }

    /** 非业务方法：检查驱动返回的执行状态，SUCCESS_NO_INFO 也表示成功。 */
    private static void verify(List<BatchResult> results) {
        for (BatchResult result : results)
            for (int n : result.getUpdateCounts())
                if (n == Statement.EXECUTE_FAILED) throw new IllegalStateException("batch failed");
    }
}
