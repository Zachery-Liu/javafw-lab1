package cn.edu.tjut.huaxin.product.util;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;

import org.apache.ibatis.reflection.MetaObject;

import java.time.LocalDateTime;

/** MP 审计字段填充：插入保留手工提供的时间，修改刷新更新时间。 */
public class ProductMetaObjectHandler implements MetaObjectHandler {
    /** 非业务方法：MP 插入时回调；strictInsertFill 不覆盖已提供的值。 */
    @Override
    public void insertFill(MetaObject meta) {
        LocalDateTime now = LocalDateTime.now();
        strictInsertFill(meta, "createdAt", LocalDateTime.class, now);
        strictInsertFill(meta, "updatedAt", LocalDateTime.class, now);
    }

    /** 非业务方法：MP 更新时回调；更新时间强制刷新，创建时间保持。 */
    @Override
    public void updateFill(MetaObject meta) {
        setFieldValByName("updatedAt", LocalDateTime.now(), meta);
    }
}
