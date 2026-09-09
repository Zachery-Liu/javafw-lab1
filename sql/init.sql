-- Product management laboratory database initialization script
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS product_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE product_management;

CREATE TABLE IF NOT EXISTS product (
    product_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品编号',
    product_code    VARCHAR(32)  NOT NULL COMMENT '商品编码',
    product_name    VARCHAR(100) NOT NULL COMMENT '商品名称',
    category        VARCHAR(50)  NOT NULL COMMENT '商品分类',
    brand           VARCHAR(50)  DEFAULT NULL COMMENT '品牌',
    unit            VARCHAR(20)  NOT NULL DEFAULT '件' COMMENT '计量单位',
    unit_price      DECIMAL(10, 2) NOT NULL COMMENT '销售单价',
    stock_quantity  INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    min_stock       INT NOT NULL DEFAULT 0 COMMENT '最低安全库存',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停售，1在售，2缺货',
    production_date DATE DEFAULT NULL COMMENT '生产日期',
    description     VARCHAR(255) DEFAULT NULL COMMENT '商品说明',
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    CONSTRAINT uk_product_code UNIQUE (product_code),
    CONSTRAINT chk_product_price CHECK (unit_price >= 0),
    CONSTRAINT chk_product_stock CHECK (stock_quantity >= 0),
    CONSTRAINT chk_product_min_stock CHECK (min_stock >= 0),
    CONSTRAINT chk_product_status CHECK (status IN (0, 1, 2)),

    INDEX idx_product_name (product_name),
    INDEX idx_product_category_status (category, status),
    INDEX idx_product_price (unit_price)
) ENGINE = InnoDB COMMENT = '商品信息表';

-- INSERT IGNORE makes the seed section safe to execute repeatedly because
-- product_code is unique.
INSERT IGNORE INTO product
    (product_code, product_name, category, brand, unit, unit_price,
     stock_quantity, min_stock, status, production_date, description)
VALUES
    ('DIGI-KEY-001', '机械键盘 K87', '数码配件', 'KeyMaster', '个',
     299.00, 48, 10, 1, '2026-03-15', '87键有线机械键盘'),
    ('DIGI-MOU-002', '无线办公鼠标 M20', '数码配件', 'MoveTech', '个',
     89.90, 6, 15, 1, '2026-04-02', '静音按键，库存低于安全值'),
    ('OFFI-PEN-003', '黑色中性笔 0.5mm', '办公用品', '文星', '盒',
     24.50, 120, 30, 1, '2026-01-10', '每盒12支'),
    ('OFFI-NOT-004', 'A5硬面笔记本', '办公用品', '纸间', '本',
     18.00, 75, 20, 1, '2026-02-18', '共160页'),
    ('HOME-CUP-005', '不锈钢保温杯 500ml', '生活用品', 'WarmDay', '个',
     129.00, 32, 8, 1, '2026-05-06', '食品接触级内胆'),
    ('HOME-LAM-006', 'LED护眼台灯', '生活用品', 'BrightHome', '台',
     159.00, 0, 10, 2, '2026-03-28', '三档色温，当前缺货'),
    ('BOOK-JAV-007', 'Java框架技术实践教程', '图书', '启航教育', '本',
     68.00, 45, 12, 1, '2025-11-01', 'MyBatis与SSM实践参考书'),
    ('FOOD-MIL-008', '纯牛奶 250ml×12', '食品饮料', '牧原优选', '箱',
     59.90, 26, 10, 1, '2026-08-20', '常温保存'),
    ('FOOD-TEA-009', '茉莉花茶 500ml', '食品饮料', '清泉', '瓶',
     5.50, 0, 24, 2, '2026-08-25', '整箱规格为15瓶'),
    ('DIGI-HUB-010', 'USB-C八合一扩展坞', '数码配件', 'LinkPro', '个',
     239.00, 18, 6, 1, '2026-06-12', '支持HDMI与千兆网口'),
    ('OFFI-CHR-011', '人体工学办公椅', '办公用品', 'SitWell', '把',
     899.00, 4, 5, 1, '2025-12-05', '低库存大件商品'),
    ('DIGI-CAM-012', 'USB高清摄像头', '数码配件', 'VisionGo', '个',
     199.00, 12, 6, 0, '2024-09-16', '旧型号，已停止销售');
