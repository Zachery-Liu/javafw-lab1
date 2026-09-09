DROP TABLE IF EXISTS product;

CREATE TABLE product (
    product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_code VARCHAR(32) NOT NULL UNIQUE,
    product_name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    brand VARCHAR(50),
    unit VARCHAR(20) NOT NULL DEFAULT '件',
    unit_price DECIMAL(10, 2) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    min_stock INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    production_date DATE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO product
    (product_code, product_name, category, brand, unit, unit_price,
     stock_quantity, min_stock, status, production_date, description)
VALUES
    ('TEST-001', '测试键盘', '数码配件', 'TestBrand', '个', 199.00, 20, 5, 1, '2026-01-10', 'CRUD测试数据'),
    ('TEST-002', '测试笔记本', '办公用品', 'TestBrand', '本', 15.50, 30, 10, 1, '2026-02-20', '查询测试数据'),
    ('TEST-003', '测试台灯', '生活用品', 'TestBrand', '台', 89.00, 0, 8, 2, '2026-03-15', '缺货测试数据');
