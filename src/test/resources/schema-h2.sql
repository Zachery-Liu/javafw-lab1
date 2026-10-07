DROP TABLE IF EXISTS product_tag_link;
DROP TABLE IF EXISTS product_detail;
DROP TABLE IF EXISTS product_tag;
-- 仅用于 H2 测试：每个测试前重建表，禁止当作正式库的升级脚本执行。
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;
DROP TABLE IF EXISTS product_brand;

CREATE TABLE product_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE product_brand (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand_name VARCHAR(50) NOT NULL UNIQUE
);
INSERT INTO product_category (id, category_name) VALUES
    (1, '数码配件'), (2, '办公用品'), (3, '生活用品'), (4, '测试分类');
INSERT INTO product_brand (id, brand_name) VALUES (1, 'TestBrand');

-- 保留与实体匹配的列；不完全复制 MySQL 索引与 ON UPDATE 等特性。
CREATE TABLE product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(32) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_id BIGINT,
    unit VARCHAR(20) NOT NULL DEFAULT '件',
    unit_price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    min_stock INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    version INT NOT NULL DEFAULT 0,
    production_date DATE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES product_category(id),
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id) REFERENCES product_brand(id)
);

-- 固定三条数据：键盘与笔记本在售，台灯缺货，deleted 默认 0。
INSERT INTO product
    (product_code, product_name, category_id, brand_id, unit, unit_price,
     stock_quantity, min_stock, status, production_date, description)
VALUES
    ('TEST-001', '测试键盘', 1, 1, '个', 199.00, 20, 5, 1, '2026-01-10', 'CRUD测试数据'),
    ('TEST-002', '测试笔记本', 2, 1, '本', 15.50, 30, 10, 1, '2026-02-20', '查询测试数据'),
    ('TEST-003', '测试台灯', 3, 1, '台', 89.00, 0, 8, 2, '2026-03-15', '缺货测试数据');

CREATE TABLE IF NOT EXISTS product_detail (
    product_id BIGINT PRIMARY KEY,
    origin VARCHAR(100) NOT NULL,
    warranty_months INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_detail_product FOREIGN KEY(product_id) REFERENCES product(product_id)
);
CREATE TABLE IF NOT EXISTS product_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tag_name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS product_tag_link (
    product_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    PRIMARY KEY(product_id, tag_id),
    CONSTRAINT fk_link_product FOREIGN KEY(product_id) REFERENCES product(product_id),
    CONSTRAINT fk_link_tag FOREIGN KEY(tag_id) REFERENCES product_tag(id)
);

INSERT INTO product_detail VALUES (1,'天津',24);
INSERT INTO product_tag(id,tag_name) VALUES (1,'热销'),(2,'办公'),(3,'空标签');
INSERT INTO product_tag_link VALUES (1,1),(1,2),(2,2);
