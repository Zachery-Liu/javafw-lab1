package cn.edu.tjut.huaxin.product.util;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.mapper.ProductMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.Map;

/** 防止商品更新遗漏版本而绕过 MP 乐观锁；限定商品更新，不干扰删除或其他表。 */
public final class ProductVersionGuardInterceptor implements InnerInterceptor {
    @Override
    public void beforeUpdate(Executor executor, MappedStatement statement, Object parameter) {
        String id = statement.getId();
        String namespace = "cn.edu.tjut.huaxin.product.mapper.";
        if (id.equals(namespace + "ProductPlusMapper.updateById")
                || id.equals(namespace + "ProductPlusMapper.update")) {
            Object entity = parameter instanceof Map
                    ? ((Map<?, ?>) parameter).get(Constants.ENTITY) : parameter;
            ProductMapper.requireVersion(entity instanceof Product ? (Product) entity : null);
        } else if (id.equals(namespace + "ProductMapper.updateRow")
                || id.equals(namespace + "ProductMapper.updateDynamicFields")) {
            ProductMapper.requireVersion(parameter instanceof Product ? (Product) parameter : null);
        }
    }
}
