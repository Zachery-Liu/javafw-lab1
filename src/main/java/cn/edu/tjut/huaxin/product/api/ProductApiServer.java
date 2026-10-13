package cn.edu.tjut.huaxin.product.api;

import cn.edu.tjut.huaxin.product.entity.Product;
import cn.edu.tjut.huaxin.product.mapper.*;
import cn.edu.tjut.huaxin.product.query.ProductQuery;
import cn.edu.tjut.huaxin.product.util.MyBatisUtil;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.net.httpserver.*;

import org.apache.ibatis.session.SqlSession;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;

/** ApiFox 本地只读入口：JDK HttpServer，绑定回环地址，每次请求创建独立会话。 */
public final class ProductApiServer {
    private ProductApiServer() {}

    /** 非接口入口：命令行启动本地服务；参数为端口，默认 8080。 */
    public static void main(String[] args) throws IOException {
        int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
        start(port, MyBatisUtil::openSession);
        System.out.println("Product API: http://127.0.0.1:" + port + "/products");
    }

    /** 非接口方法：生产入口和自动化测试复用启动逻辑；测试可用端口 0 自动分配。 */
    public static HttpServer start(int port, Supplier<SqlSession> sessions) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/products", exchange -> handle(exchange, sessions));
        server.start();
        return server;
    }

    /** 非测试回调：验证请求和分页参数，选择列表或详情，统一输出 HTTP 状态。 */
    private static void handle(HttpExchange exchange, Supplier<SqlSession> sessions)
            throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Allow", "GET");
            send(exchange, 405, "{\"error\":\"GET required\"}");
            return;
        }
        try (SqlSession session = sessions.get()) {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/products")) {
                Map<String, String> args = query(exchange.getRequestURI().getRawQuery());
                int page = Integer.parseInt(args.getOrDefault("page", "1")),
                        size = Integer.parseInt(args.getOrDefault("size", "3"));
                if (page < 1
                        || size < 1
                        || size > 100
                        || (long) (page - 1) * size > Integer.MAX_VALUE)
                    throw new IllegalArgumentException();
                ProductQuery q = new ProductQuery();
                q.setProductName(args.get("name"));
                if (args.containsKey("categoryId"))
                    q.setCategoryId(Long.valueOf(args.get("categoryId")));
                if (args.containsKey("status")) {
                    int status = Integer.parseInt(args.get("status"));
                    if (status < 0 || status > 2) throw new IllegalArgumentException();
                    q.setStatus(status);
                }
                IPage<Product> result =
                        session.getMapper(ProductPlusMapper.class)
                                .selectPageByCondition(new Page<Product>(page, size), q);
                StringJoiner items = new StringJoiner(",", "[", "]");
                for (Product product : result.getRecords()) items.add(json(product));
                send(
                        exchange,
                        200,
                        "{\"total\":"
                                + result.getTotal()
                                + ",\"page\":"
                                + page
                                + ",\"size\":"
                                + size
                                + ",\"data\":"
                                + items
                                + "}");
            } else if (path.matches("/products/[0-9]+")) {
                Product p =
                        session.getMapper(ProductMapper.class)
                                .selectById(Long.valueOf(path.substring(10)));
                send(
                        exchange,
                        p == null ? 404 : 200,
                        p == null ? "{\"error\":\"not found\"}" : json(p));
            } else send(exchange, 404, "{\"error\":\"not found\"}");
        } catch (IllegalArgumentException ex) {
            send(exchange, 400, "{\"error\":\"invalid query\"}");
        } catch (Exception ex) {
            send(exchange, 500, "{\"error\":\"database operation failed\"}");
        }
    }

    /** 非接口方法：URL 参数按 UTF-8 解码，传给 Mapper 后仍使用 SQL 绑定。 */
    private static Map<String, String> query(String raw) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (raw == null) return map;
        for (String pair : raw.split("&")) {
            String[] p = pair.split("=", 2);
            map.put(
                    URLDecoder.decode(p[0], "UTF-8"),
                    p.length == 1 ? "" : URLDecoder.decode(p[1], "UTF-8"));
        }
        return map;
    }

    /** 非接口方法：把本次查到的商品转换为 JSON，不触发额外数据库查询。 */
    private static String json(Product p) {
        return "{\"productId\":"
                + p.getProductId()
                + ",\"productName\":"
                + quote(p.getProductName())
                + ",\"category\":"
                + quote(p.getCategory())
                + ",\"brand\":"
                + quote(p.getBrand())
                + ",\"unitPrice\":"
                + p.getUnitPrice().toPlainString()
                + ",\"version\":"
                + p.getVersion()
                + "}";
    }

    /** 非接口方法：JSON 字符串转义控制符、引号和反斜线，防止商品名称破坏响应结构。 */
    private static String quote(String value) {
        if (value == null) return "null";
        StringBuilder b = new StringBuilder("\"");
        for (char c : value.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\').append(c);
            else if (c < 32) b.append(String.format("\\u%04x", (int) c));
            else b.append(c);
        }
        return b.append('"').toString();
    }

    /** 非接口方法：统一设置 UTF-8 JSON 响应并关闭输出流。 */
    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
