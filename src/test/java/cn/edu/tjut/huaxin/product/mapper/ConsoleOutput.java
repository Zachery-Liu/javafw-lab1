package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

/** 非测试辅助类：只打印已经得到的数据，不查库、不提交事务，不增加测试数量。 */
public final class ConsoleOutput {
    /** 非测试构造方法：工具类通过静态方法调用。 */
    private ConsoleOutput() {}

    /** 非测试方法：交给 @Rule，由 JUnit 在开始、全部断言成功或失败时回调。 */
    public static TestWatcher watch() {
        return new TestWatcher() {
            @Override
            protected void starting(Description description) {
                System.out.println("\n========== 开始测试：" + description.getClassName()
                        + "." + description.getMethodName() + " ==========");
            }

            @Override
            protected void succeeded(Description description) {
                System.out.println("【测试通过】操作及断言全部完成：" + description.getMethodName());
            }

            @Override
            protected void failed(Throwable error, Description description) {
                System.out.println("【测试失败】" + description.getMethodName()
                        + "；异常类型=" + error.getClass().getSimpleName());
            }
        };
    }

    /** 非测试方法：打印准备执行的操作，不表示已经成功。 */
    public static void operation(String description) {
        System.out.println("【本次操作】" + description);
    }

    /** 非测试方法：打印事务提交、版本冲突等说明。 */
    public static void info(String description) {
        System.out.println("【说明】" + description);
    }

    /** 非测试方法：打印并返回影响行数，供原断言使用；不重复执行写操作。 */
    public static int rows(String label, int rows) {
        System.out.println("【操作结果】" + label + "；实际影响行数=" + rows
                + "（事务是否提交以 commit 为准）");
        return rows;
    }

    /** 非测试方法：打印并返回原商品对象；null 表示未查询到可见记录。 */
    public static Product product(String label, Product product) {
        if (product == null) {
            System.out.println("【商品结果】" + label + "：未查到可见商品。");
        } else {
            System.out.println("【商品结果】" + label + "：编号=" + product.getProductId()
                    + "，编码=" + product.getProductCode() + "，名称=" + product.getProductName()
                    + "，分类编号=" + product.getCategoryId() + "，分类=" + product.getCategory()
                    + "，品牌编号=" + product.getBrandId() + "，品牌=" + product.getBrand()
                    + "，单价=" + product.getUnitPrice() + "，库存=" + product.getStockQuantity()
                    + "，状态=" + product.getStatus() + "，删除标记=" + product.getDeleted()
                    + "，版本=" + product.getVersion());
        }
        return product;
    }

    /** 非测试方法：打印列表条数及已有商品，不重新查询。 */
    public static List<Product> products(String label, List<Product> products) {
        System.out.println("【列表结果】" + label + "；实际返回条数=" + products.size());
        for (int index = 0; index < products.size(); index++) {
            product("第 " + (index + 1) + " 条", products.get(index));
        }
        return products;
    }

    /** 非测试方法：打印已得到的分页信息和本页商品，返回原结果。 */
    public static IPage<Product> page(String label, IPage<Product> page) {
        System.out.println("【分页结果】" + label + "；当前页=" + page.getCurrent()
                + "，每页条数=" + page.getSize() + "，符合条件总数=" + page.getTotal()
                + "，总页数=" + page.getPages());
        products("当前页商品", page.getRecords());
        return page;
    }

    /** 非测试方法：打印本次输入的查询条件，不执行 SQL。 */
    public static void criteria(ProductQuery query) {
        System.out.println("【查询条件】名称=" + query.getProductName() + "，状态=" + query.getStatus()
                + "，分类编号=" + query.getCategoryId() + "，品牌编号=" + query.getBrandId()
                + "，最低价=" + query.getMinPrice() + "，最高价=" + query.getMaxPrice());
    }
}
