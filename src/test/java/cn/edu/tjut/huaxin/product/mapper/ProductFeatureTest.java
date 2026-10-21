package cn.edu.tjut.huaxin.product.mapper;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.entity.ProductGraph;
import cn.edu.tjut.huaxin.product.entity.CategoryGraph;
import cn.edu.tjut.huaxin.product.entity.TagGraph;
import cn.edu.tjut.huaxin.product.cache.TeachingCacheMapper;
import cn.edu.tjut.huaxin.product.cache.IndependentCacheMapper;
import cn.edu.tjut.huaxin.product.api.ProductApiServer;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import com.sun.net.httpserver.HttpServer;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/** 商品功能验证：注解、关联映射、延迟加载、缓存和本地 HTTP 接口。 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductFeatureTest extends AbstractH2MapperTest {

    /** 关联映射：验证三表联查、一对一、一对多、多对多、空关联及删除过滤。 */
    @Test
    public void testRelationMappings() throws Exception {
        // 三表查询：商品 JOIN 分类和品牌，返回对象直接带有分类名、品牌名。
        {
            ConsoleOutput.operation("三表联查：商品同时带出分类名和品牌名。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Product product = ConsoleOutput.product("商品、分类、品牌联查",
                        session.getMapper(ProductMapper.class).selectById(1L));
                Assert.assertNotNull(product);
                Assert.assertEquals("数码配件", product.getCategory());
                Assert.assertEquals("TestBrand", product.getBrand());
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 联合结果映射：两条标签行应归并成同一个商品对象和两个标签。
        {
            ConsoleOutput.operation("一次联合查询映射分类、品牌、详情和两个标签。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductGraph graph = session.getMapper(ProductRelationMapper.class).selectGraph(1L);
                assertProductGraph(graph);
                queryCounter.expect("多行标签归并为一个商品", 1);
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 集合关系：一对多、多对多、空分类和空标签都应保留正确集合。
        {
            ConsoleOutput.operation("检查分类商品集合、标签商品集合和没有商品的父对象。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductRelationMapper mapper = session.getMapper(ProductRelationMapper.class);
                CategoryGraph category = mapper.selectCategory(1L);
                Assert.assertEquals("数码配件", category.getCategoryName());
                Assert.assertEquals(Long.valueOf(1), category.getProducts().get(0).getProductId());
                Assert.assertEquals(1, category.getProducts().size());
                Assert.assertTrue(mapper.selectCategory(4L).getProducts().isEmpty());
                TagGraph tag = mapper.selectTag(2L);
                Assert.assertEquals("办公", tag.getTagName());
                Assert.assertEquals(Arrays.asList(1L, 2L), tag.getProducts().stream().map(Product::getProductId).collect(Collectors.toList()));
                Assert.assertTrue(mapper.selectTag(3L).getProducts().isEmpty());
                Assert.assertNull(mapper.selectCategory(999L));
                Assert.assertNull(mapper.selectTag(999L));
                ConsoleOutput.info("分类 1 有 1 件商品，办公标签有 2 件商品，空分类和空标签返回空集合。");
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 空关联和删除：没有品牌/详情也返回商品；删除记录不进入关联集合。
        {
            ConsoleOutput.operation("核对空品牌、空详情和逻辑删除后的关联查询。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                try (java.sql.Statement statement = session.getConnection().createStatement()) {
                    Assert.assertEquals(1, statement.executeUpdate("UPDATE product SET brand_id=NULL WHERE product_id=2"));
                }
                session.commit(true);
                ProductRelationMapper mapper = session.getMapper(ProductRelationMapper.class);
                ProductGraph second = mapper.selectGraph(2L);
                Assert.assertNotNull(second);
                Assert.assertNull(second.getBrandObject());
                Assert.assertNull(second.getDetail());
                Assert.assertEquals(1, second.getTags().size());
                Assert.assertTrue(mapper.selectGraph(3L).getTags().isEmpty());
                session.getMapper(ProductMapper.class).deleteById(1L);
                session.commit();
                Assert.assertNull(mapper.selectGraph(1L));
                Assert.assertTrue(mapper.selectCategory(1L).getProducts().isEmpty());
                Assert.assertTrue(mapper.selectTag(1L).getProducts().isEmpty());
                ConsoleOutput.info("空关联未生成虚假对象，已删除商品未进入分类及标签集合。");
            }
        }
    }

    /** 注解与 Provider：验证 CRUD、动态条件、参数绑定及 XML 混合映射。 */
    @Test
    public void testAnnotationCrudAndProvider() throws Exception {
        // 注解 CRUD：新增回填、版本更新、冲突拒绝及逻辑删除均真实执行。
        {
            ConsoleOutput.operation("注解方式新增、查询、修改和逻辑删除商品。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductAnnotationMapper mapper = session.getMapper(ProductAnnotationMapper.class);
                Product product = newProduct("ANNOTATION-001");
                Assert.assertEquals(1, ConsoleOutput.rows("注解新增", mapper.insert(product)));
                session.commit();
                Assert.assertNotNull(product.getProductId());
                Product stored = ConsoleOutput.product("注解查询新增商品", mapper.selectById(product.getProductId()));
                Assert.assertEquals("ANNOTATION-001", stored.getProductCode());
                Assert.assertEquals(Integer.valueOf(0), stored.getVersion());
                stored.setProductName("注解修改名称");
                Assert.assertEquals(1, ConsoleOutput.rows("注解修改", mapper.updateName(stored)));
                Assert.assertEquals(0, ConsoleOutput.rows("注解旧版本修改", mapper.updateName(stored)));
                session.commit();
                Product updated = mapper.selectById(product.getProductId());
                Assert.assertEquals("注解修改名称", updated.getProductName());
                Assert.assertEquals(Integer.valueOf(1), updated.getVersion());
                Assert.assertEquals(1, ConsoleOutput.rows("注解逻辑删除", mapper.deleteById(product.getProductId())));
                Assert.assertEquals(0, mapper.deleteById(product.getProductId()));
                session.commit();
                Assert.assertNull(mapper.selectById(product.getProductId()));
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // Provider：检查空条件、组合条件、结果映射以及输入作为参数而非 SQL 片段。
        {
            ConsoleOutput.operation("运行 SQL Provider，验证空条件、组合条件和带 SQL 字符的名称。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductAnnotationMapper mapper = session.getMapper(ProductAnnotationMapper.class);
                Assert.assertEquals(3, mapper.selectByCondition(null).size());
                Assert.assertEquals(3, mapper.selectByCondition(new ProductQuery()).size());
                ProductQuery q = new ProductQuery();
                q.setProductName("键盘");
                q.setCategoryId(1L);
                q.setStatus(1);
                Product stored = ConsoleOutput.products("Provider 查询结果", mapper.selectByCondition(q)).get(0);
                Assert.assertEquals(Long.valueOf(1), stored.getProductId());
                Assert.assertEquals("TEST-001", stored.getProductCode());
                q.setStatus(2);
                Assert.assertTrue(mapper.selectByCondition(q).isEmpty());
                q.setStatus(null);
                q.setProductName("' OR 1=1 --");
                Assert.assertTrue(mapper.selectByCondition(q).isEmpty());
                ConsoleOutput.info("特殊字符作为名称参数处理，没有改变查询条件结构。");
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 参数与混合开发：Map、多 @Param 及同一 Mapper 的 XML 联查结果一致。
        {
            ConsoleOutput.operation("对比 Map、多参数注解查询和同一接口中的 XML 联表查询。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductAnnotationMapper mapper = session.getMapper(ProductAnnotationMapper.class);
                Map<String, Object> values = new HashMap<>();
                values.put("categoryId", 1L);
                values.put("status", 1);
                Assert.assertEquals("TEST-001", mapper.selectByMap(values).get(0).getProductCode());
                Assert.assertEquals("TEST-001", mapper.selectByParams(1L, 1).get(0).getProductCode());
                Assert.assertTrue(mapper.selectByParams(1L, 2).isEmpty());
                Product joined = ConsoleOutput.product("注解接口的 XML 联查", mapper.selectJoinedById(1L));
                Assert.assertEquals("数码配件", joined.getCategory());
                Assert.assertEquals("TestBrand", joined.getBrand());
            }
        }
    }

    /** 延迟加载：验证 XML 与注解关联，统计访问属性前后的实际查询次数。 */
    @Test
    public void testLazyLoading() throws Exception {
        // XML 延迟加载：每访问一个关联才增加一次 SQL，重复访问不再查询。
        {
            ConsoleOutput.operation("统计主查询和逐个访问关联属性时的真实 SELECT 次数。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductGraph graph = session.getMapper(ProductRelationMapper.class).selectLazyGraph(1L);
                Assert.assertEquals(Long.valueOf(1), graph.getProductId());
                queryCounter.expect("只读取主对象", 1);
                Assert.assertEquals("天津", graph.getDetail().getOrigin());
                queryCounter.expect("访问详情后", 2);
                Assert.assertEquals(Integer.valueOf(24), graph.getDetail().getWarrantyMonths());
                queryCounter.expect("重复访问详情", 2);
                Assert.assertEquals("数码配件", graph.getCategoryObject().getCategoryName());
                queryCounter.expect("访问分类后", 3);
                Assert.assertEquals("TestBrand", graph.getBrandObject().getBrandName());
                queryCounter.expect("访问品牌后", 4);
                Assert.assertEquals(2, graph.getTags().size());
                queryCounter.expect("访问标签后", 5);
                Assert.assertEquals(2, graph.getTags().size());
                queryCounter.expect("重复访问标签", 5);
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 注解关联：验证 @One、@Many、延迟查询和分类/标签的反向集合。
        {
            ConsoleOutput.operation("用注解映射完整关联对象，并验证延迟加载和反向集合。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductRelationAnnotationMapper mapper = session.getMapper(ProductRelationAnnotationMapper.class);
                ProductGraph graph = mapper.selectGraph(1L);
                queryCounter.expect("注解关联尚未访问", 1);
                assertProductGraph(graph);
                queryCounter.expect("四个关联按需查询后", 5);
                Assert.assertEquals(1, mapper.selectCategory(1L).getProducts().size());
                Assert.assertEquals(2, mapper.selectTag(2L).getProducts().size());
                Assert.assertTrue(mapper.selectCategory(4L).getProducts().isEmpty());
                Assert.assertTrue(mapper.selectTag(3L).getProducts().isEmpty());
            }
        }
    }

    /** MP 乐观锁：两个会话读取相同版本，先提交成功，旧版本更新影响 0 行。 */
    @Test
    public void testOptimisticLock() throws Exception {
        ConsoleOutput.operation("两个会话读取同一版本，先提交者成功，旧版本更新失败。");
        try (SqlSession first = sqlSessionFactory.openSession();
             SqlSession second = sqlSessionFactory.openSession()) {
            ProductPlusMapper firstMapper = first.getMapper(ProductPlusMapper.class);
            ProductPlusMapper secondMapper = second.getMapper(ProductPlusMapper.class);
            Product firstCopy = ConsoleOutput.product("会话一读取", firstMapper.selectById(1L));
            Product secondCopy = ConsoleOutput.product("会话二读取", secondMapper.selectById(1L));
            firstCopy.setProductName("先提交的名称");
            Assert.assertEquals(1, ConsoleOutput.rows("会话一更新", firstMapper.updateById(firstCopy)));
            first.commit();
            ConsoleOutput.info("会话一已提交，新版本=" + firstCopy.getVersion());
            secondCopy.setProductName("旧版本想覆盖的名称");
            Assert.assertEquals(0, ConsoleOutput.rows("会话二用旧版本更新", secondMapper.updateById(secondCopy)));
            second.commit();
            second.clearCache();
            Product stored = ConsoleOutput.product("刷新后数据库中的商品", secondMapper.selectById(1L));
            Assert.assertEquals("先提交的名称", stored.getProductName());
            ConsoleOutput.info("旧版本影响 0 行，数据库保留先提交的名称。");
        }
    }

    /** 一级缓存：检查重复查询、手动清理、更新、提交、回滚和跨会话隔离。 */
    @Test
    public void testFirstLevelCache() throws Exception {
        // 一级缓存：同一个会话内重复查询同一 ID，返回缓存中的同一个对象。
        {
            ConsoleOutput.operation("同一个会话中，连续两次按编号 1 查询商品。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                Product first = ConsoleOutput.product("第一次查询", mapper.selectById(1L));
                Product second = ConsoleOutput.product("第二次查询", mapper.selectById(1L));
                Assert.assertNotNull(first);
                Assert.assertSame(first, second);
                queryCounter.expect("同一会话重复查询命中一级缓存", 1);
                ConsoleOutput.info("两次结果是否为同一对象=" + (first == second) + "；观察 SQL 日志只有一次 SELECT。");
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 一级缓存生命周期：手动清理、更新、提交、回滚和独立会话都真实触发查询。
        {
            ConsoleOutput.operation("统计 clearCache、更新、提交、回滚及跨会话后的查询次数。");
            Product last;
            try (SqlSession session = sqlSessionFactory.openSession()) {
                ProductMapper mapper = session.getMapper(ProductMapper.class);
                Product first = mapper.selectById(1L);
                Assert.assertSame(first, mapper.selectById(1L));
                queryCounter.expect("重复查询", 1);
                session.clearCache();
                Product cleared = mapper.selectById(1L);
                Assert.assertNotSame(first, cleared);
                queryCounter.expect("clearCache 后", 2);
                cleared.setProductName("一级缓存更新名称");
                Assert.assertEquals(1, mapper.updateDynamic(cleared));
                Assert.assertEquals("一级缓存更新名称", mapper.selectById(1L).getProductName());
                queryCounter.expect("更新后", 3);
                session.commit();
                last = mapper.selectById(1L);
                queryCounter.expect("提交后", 4);
                session.rollback();
                mapper.selectById(1L);
                queryCounter.expect("回滚后", 5);
            }
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Assert.assertNotSame(last, session.getMapper(ProductMapper.class).selectById(1L));
                queryCounter.expect("新会话不共享一级缓存", 6);
            }
        }
    }

    /** 二级缓存：验证跨会话命中、更新失效、绕过、刷新、语句和命名空间隔离及回滚。 */
    @Test
    public void testSecondLevelCache() throws Exception {
        // 二级缓存命中：两个独立会话共享结果；readWrite 模式返回对象副本。
        {
            ConsoleOutput.operation("会话一查询并提交，会话二读取二级缓存，不再次执行 SQL。");
            QueryCounter counter = new QueryCounter();
            SqlSessionFactory factory = cacheFactory(counter);
            Product first = cachedProduct(factory);
            counter.expect("首个会话访问数据库", 1);
            Product second = cachedProduct(factory);
            counter.expect("第二个会话命中二级缓存", 1);
            Assert.assertEquals(first.getProductCode(), second.getProductCode());
            Assert.assertNotSame(first, second);
            second.setProductName("仅修改缓存副本");
            Assert.assertEquals("测试键盘", cachedProduct(factory).getProductName());
            counter.expect("副本修改未污染缓存", 1);
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 二级缓存失效：同一命名空间提交更新后，新会话重新读取数据库。
        {
            ConsoleOutput.operation("预热二级缓存，提交商品更新，检查旧缓存失效。");
            QueryCounter counter = new QueryCounter();
            SqlSessionFactory factory = cacheFactory(counter);
            cachedProduct(factory);
            try (SqlSession session = factory.openSession()) {
                TeachingCacheMapper mapper = session.getMapper(TeachingCacheMapper.class);
                Product product = mapper.selectById(1L);
                product.setProductName("二级缓存更新名称");
                Assert.assertEquals(1, ConsoleOutput.rows("缓存 Mapper 更新", mapper.updateName(product)));
                session.commit();
            }
            counter.expect("更新前的读取命中缓存", 1);
            Product stored = cachedProduct(factory);
            Assert.assertEquals("二级缓存更新名称", stored.getProductName());
            Assert.assertEquals(Integer.valueOf(1), stored.getVersion());
            counter.expect("更新提交后重新执行查询", 2);
            cachedProduct(factory);
            counter.expect("更新后的结果再次缓存", 2);
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 缓存选项：useCache=false 只绕过二级缓存；强制刷新清除旧结果。
        {
            ConsoleOutput.operation("验证绕过二级缓存仍保留一级缓存，以及强制刷新行为。");
            QueryCounter counter = new QueryCounter();
            SqlSessionFactory factory = cacheFactory(counter);
            cachedProduct(factory);
            try (SqlSession session = factory.openSession()) {
                TeachingCacheMapper mapper = session.getMapper(TeachingCacheMapper.class);
                Product first = mapper.selectUncached(1L);
                Assert.assertSame(first, mapper.selectUncached(1L));
                counter.expect("绕过二级缓存但命中一级缓存", 2);
            }
            cachedProduct(factory);
            counter.expect("原有二级缓存仍可用", 2);
            try (SqlSession session = factory.openSession()) {
                Assert.assertNotNull(session.getMapper(TeachingCacheMapper.class).selectAndFlush(1L));
                session.commit();
            }
            counter.expect("强制刷新查询", 3);
            cachedProduct(factory);
            counter.expect("刷新后旧语句缓存失效", 4);
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 语句标识：即使 SQL 相同，不同方法仍使用不同缓存键。
        {
            ConsoleOutput.operation("相同 SQL 的不同 Mapper 方法不直接共用查询缓存键。");
            QueryCounter counter = new QueryCounter();
            SqlSessionFactory factory = cacheFactory(counter);
            cachedProduct(factory);
            for (int i = 0; i < 2; i++) {
                try (SqlSession session = factory.openSession()) {
                    Assert.assertNotNull(session.getMapper(TeachingCacheMapper.class).selectSameSql(1L));
                    session.commit();
                }
                counter.expect("相同 SQL 的另一个方法，第 " + (i + 1) + " 次访问", 2);
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 命名空间隔离：更新一个 Mapper 不会自动清除另一个 Mapper 的缓存。
        {
            ConsoleOutput.operation("对比两个命名空间，展示跨 Mapper 旧缓存的实际风险。");
            QueryCounter counter = new QueryCounter();
            SqlSessionFactory factory = cacheFactory(counter);
            cachedProduct(factory);
            try (SqlSession session = factory.openSession()) {
                Assert.assertEquals("测试键盘", session.getMapper(IndependentCacheMapper.class).selectById(1L).getProductName());
                session.commit();
            }
            counter.expect("两个命名空间各执行一次查询", 2);
            try (SqlSession session = factory.openSession()) {
                TeachingCacheMapper mapper = session.getMapper(TeachingCacheMapper.class);
                Product product = mapper.selectById(1L);
                product.setProductName("仅刷新当前命名空间");
                Assert.assertEquals(1, mapper.updateName(product));
                session.commit();
            }
            Assert.assertEquals("仅刷新当前命名空间", cachedProduct(factory).getProductName());
            try (SqlSession session = factory.openSession()) {
                Assert.assertEquals("测试键盘", session.getMapper(IndependentCacheMapper.class).selectById(1L).getProductName());
            }
            counter.expect("独立命名空间仍读取旧缓存", 3);
            ConsoleOutput.info("当前 Mapper 已刷新；独立 Mapper 仍持有旧值，二级缓存不会按数据表自动联动失效。");
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // 回滚与缓存：只读会话强制回滚不发布结果，写事务回滚不污染已提交结果。
        {
            ConsoleOutput.operation("检查回滚不会发布待提交缓存或缓存未提交的修改。");
            QueryCounter counter = new QueryCounter();
            SqlSessionFactory factory = cacheFactory(counter);
            try (SqlSession session = factory.openSession()) {
                session.getMapper(TeachingCacheMapper.class).selectById(1L);
                // 只读会话没有 dirty 标记，必须 force=true 才通知二级缓存事务执行回滚。
                session.rollback(true);
            }
            cachedProduct(factory);
            counter.expect("回滚后的新会话重新查库", 2);
            try (SqlSession session = factory.openSession()) {
                TeachingCacheMapper mapper = session.getMapper(TeachingCacheMapper.class);
                Product product = mapper.selectById(1L);
                product.setProductName("应回滚的缓存更新");
                Assert.assertEquals(1, mapper.updateName(product));
                session.rollback();
            }
            Assert.assertEquals("测试键盘", cachedProduct(factory).getProductName());
            counter.expect("更新回滚后仍读取已提交的旧值", 2);
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Assert.assertEquals("测试键盘", session.getMapper(ProductMapper.class).selectById(1L).getProductName());
            }
        }
    }

    /** 商品 API 正常请求：验证列表、筛选、分页、详情、响应头及 JSON 转义。 */
    @Test
    public void testProductApiQueries() throws Exception {
        // HTTP 正常路径：本地随机端口，用 H2 验证筛选、分页、联查和详情。
        {
            ConsoleOutput.operation("启动本地商品 HTTP 接口，验证列表筛选、分页和详情。");
            HttpServer server = ProductApiServer.start(0, sqlSessionFactory::openSession);
            try {
                ApiResponse list = request(server, "GET", "/products?page=1&size=2");
                Assert.assertEquals(200, list.status);
                Assert.assertTrue(list.contentType.contains("application/json"));
                Assert.assertTrue(list.contentType.toLowerCase().contains("utf-8"));
                Assert.assertTrue(list.body.contains("\"total\":3"));
                Assert.assertTrue(list.body.contains("\"productId\":1"));
                Assert.assertTrue(list.body.contains("\"productId\":3"));
                Assert.assertFalse(list.body.contains("\"productId\":2"));
                ApiResponse second = request(server, "GET", "/products?page=2&size=2");
                Assert.assertTrue(second.body.contains("\"productId\":2"));
                Assert.assertFalse(second.body.contains("\"productId\":1"));
                ApiResponse filtered = request(server, "GET", "/products?name="
                        + URLEncoder.encode("键盘", "UTF-8") + "&categoryId=1&status=1");
                Assert.assertEquals(200, filtered.status);
                Assert.assertTrue(filtered.body.contains("\"total\":1"));
                Assert.assertTrue(filtered.body.contains("\"category\":\"数码配件\""));
                Assert.assertTrue(filtered.body.contains("\"brand\":\"TestBrand\""));
                ApiResponse detail = request(server, "GET", "/products/1");
                Assert.assertEquals(200, detail.status);
                Assert.assertTrue(detail.body.contains("\"productName\":\"测试键盘\""));
                ApiResponse empty = request(server, "GET", "/products?page=3&size=2");
                Assert.assertEquals(200, empty.status);
                Assert.assertTrue(empty.body.contains("\"data\":[]"));
            } finally {
                server.stop(0);
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // HTTP 序列化：引号、反斜线及换行必须转义，中文正常保留。
        {
            ConsoleOutput.operation("检查商品名称中的引号、反斜线和换行不会破坏 JSON。");
            try (SqlSession session = sqlSessionFactory.openSession()) {
                Product product = session.getMapper(ProductMapper.class).selectById(1L);
                product.setProductName("键\"盘\\\n");
                Assert.assertEquals(1, session.getMapper(ProductMapper.class).updateDynamic(product));
                session.commit();
            }
            HttpServer server = ProductApiServer.start(0, sqlSessionFactory::openSession);
            try {
                ApiResponse response = request(server, "GET", "/products/1");
                Assert.assertEquals(200, response.status);
                Assert.assertTrue(response.body.contains("键\\\"盘\\\\\\u000a"));
                Assert.assertFalse(response.body.contains("\n"));
            } finally {
                server.stop(0);
            }
        }
    }

    /** 商品 API 异常请求：验证非法参数、不存在或删除记录、方法限制和数据库失败响应。 */
    @Test
    public void testProductApiErrors() throws Exception {
        // HTTP 边界：无效查询 400，不存在/已删除 404，不允许的方法 405。
        {
            ConsoleOutput.operation("验证商品 API 的 400、404、405 和已删除商品不可见。");
            HttpServer server = ProductApiServer.start(0, sqlSessionFactory::openSession);
            try {
                for (String query : Arrays.asList("page=0", "size=0", "size=101", "status=3", "page=abc",
                        "categoryId=abc", "page=2147483647&size=100")) {
                    Assert.assertEquals(query, 400, request(server, "GET", "/products?" + query).status);
                }
                Assert.assertEquals(404, request(server, "GET", "/products/999").status);
                Assert.assertEquals(404, request(server, "GET", "/products/unknown").status);
                ApiResponse method = request(server, "POST", "/products");
                Assert.assertEquals(405, method.status);
                Assert.assertEquals("GET", method.allow);
                try (SqlSession session = sqlSessionFactory.openSession()) {
                    session.getMapper(ProductMapper.class).deleteById(1L);
                    session.commit();
                }
                Assert.assertEquals(404, request(server, "GET", "/products/1").status);
            } finally {
                server.stop(0);
            }
        }

        // 各场景独立恢复数据，保证集合大小、版本和查询计数不受前一场景影响。
        resetDatabase();
        // HTTP 异常：模拟数据库入口失败，响应不能泄露内部异常文本。
        {
            ConsoleOutput.operation("模拟数据库访问失败，检查接口返回统一 500 响应。");
            HttpServer server = ProductApiServer.start(0, () -> { throw new IllegalStateException("internal-test-error"); });
            try {
                ApiResponse response = request(server, "GET", "/products");
                Assert.assertEquals(500, response.status);
                Assert.assertTrue(response.body.contains("database operation failed"));
                Assert.assertFalse(response.body.contains("internal-test-error"));
            } finally {
                server.stop(0);
            }
        }
    }

    /** 非测试辅助方法：核对关联对象和标签内容，不只检查对象非空。 */
    private static void assertProductGraph(ProductGraph graph) {
        Assert.assertNotNull(graph);
        Assert.assertEquals(Long.valueOf(1), graph.getProductId());
        Assert.assertEquals("数码配件", graph.getCategoryObject().getCategoryName());
        Assert.assertEquals("TestBrand", graph.getBrandObject().getBrandName());
        Assert.assertEquals("天津", graph.getDetail().getOrigin());
        Assert.assertEquals(Integer.valueOf(24), graph.getDetail().getWarrantyMonths());
        Assert.assertEquals(Arrays.asList(1L, 2L), graph.getTags().stream().map(t -> t.getId()).collect(Collectors.toList()));
        ConsoleOutput.info("分类=" + graph.getCategoryObject().getCategoryName() + "，品牌="
                + graph.getBrandObject().getBrandName() + "，产地=" + graph.getDetail().getOrigin()
                + "，保修月数=" + graph.getDetail().getWarrantyMonths() + "，标签="
                + graph.getTags().stream().map(t -> t.getTagName()).collect(Collectors.toList()));
    }

    /** 非测试辅助方法：每项二级缓存测试使用独立工厂，避免上项缓存影响结果。 */
    private static SqlSessionFactory cacheFactory(QueryCounter counter) throws Exception {
        return createFactory("mybatis-cache-test.xml", counter);
    }

    /** 非测试辅助方法：用独立会话查询并提交，确保结果发布到二级缓存。 */
    private static Product cachedProduct(SqlSessionFactory factory) {
        try (SqlSession session = factory.openSession()) {
            Product product = ConsoleOutput.product("独立会话商品查询", session.getMapper(TeachingCacheMapper.class).selectById(1L));
            Assert.assertNotNull(product);
            session.commit();
            return product;
        }
    }

    /** 非测试辅助方法：发送真实本地 HTTP 请求，记录状态、响应头及正文。 */
    private static ApiResponse request(HttpServer server, String method, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http://127.0.0.1:"
                + server.getAddress().getPort() + path).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        try {
            int status = connection.getResponseCode();
            try (InputStream input = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                if (input != null) while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
                ApiResponse response = new ApiResponse(status, output.toString("UTF-8"),
                        connection.getHeaderField("Content-Type"), connection.getHeaderField("Allow"));
                ConsoleOutput.info(method + " " + path + "，HTTP=" + status + "，响应=" + response.body);
                return response;
            }
        } finally {
            connection.disconnect();
        }
    }

    /** 非测试数据容器：保存本次 HTTP 响应，便于直接断言实际返回内容。 */
    private static final class ApiResponse {
        final int status;
        final String body;
        final String contentType;
        final String allow;

        ApiResponse(int status, String body, String contentType, String allow) {
            this.status = status;
            this.body = body;
            this.contentType = contentType;
            this.allow = allow;
        }
    }
}
