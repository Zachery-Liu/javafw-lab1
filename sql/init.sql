-- 商品管理实验的新环境初始化脚本，适用于 MySQL 8.0 及以上。
-- 此脚本创建示例库 product_management；已有本机 javafw 库升级请使用迁移脚本。

CREATE DATABASE IF NOT EXISTS product_management
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE product_management;

-- 分类、品牌分别独立维护，一条分类/品牌记录可被多个商品引用。
CREATE TABLE IF NOT EXISTS product_category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '分类编号',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    CONSTRAINT uk_category_name UNIQUE (category_name)
) ENGINE = InnoDB COMMENT = '商品分类表';

CREATE TABLE IF NOT EXISTS product_brand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '品牌编号',
    brand_name VARCHAR(50) NOT NULL COMMENT '品牌名称',
    CONSTRAINT uk_brand_name UNIQUE (brand_name)
) ENGINE = InnoDB COMMENT = '商品品牌表';

INSERT IGNORE INTO product_category (category_name)
VALUES ('数码配件'), ('办公用品'), ('生活用品'), ('图书'), ('食品饮料');
INSERT IGNORE INTO product_brand (brand_name)
VALUES ('KeyMaster'), ('MoveTech'), ('文星'), ('纸间'), ('WarmDay'), ('BrightHome'),
       ('启航教育'), ('牧原优选'), ('清泉'), ('LinkPro'), ('SitWell'), ('VisionGo');

-- IF NOT EXISTS 不会为旧表新增字段；主键自增，商品编码唯一，deleted 独立于 status。
CREATE TABLE IF NOT EXISTS product (
    product_id      BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '商品编号',
    product_code    VARCHAR(32)  NOT NULL COMMENT '商品编码',
    product_name    VARCHAR(100) NOT NULL COMMENT '商品名称',
    category_id     BIGINT NOT NULL COMMENT '分类外键',
    brand_id        BIGINT DEFAULT NULL COMMENT '品牌外键，可为空',
    unit            VARCHAR(20)  NOT NULL DEFAULT '件' COMMENT '计量单位',
    unit_price      DECIMAL(10, 2) NOT NULL COMMENT '销售单价',
    stock_quantity  INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    min_stock       INT NOT NULL DEFAULT 0 COMMENT '最低安全库存',
    status          TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停售，1在售，2缺货',
    deleted         TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    version         INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本，每次更新递增',
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
    CONSTRAINT chk_product_deleted CHECK (deleted IN (0, 1)),

    CONSTRAINT fk_product_category FOREIGN KEY (category_id)
        REFERENCES product_category(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_product_brand FOREIGN KEY (brand_id)
        REFERENCES product_brand(id) ON DELETE RESTRICT ON UPDATE RESTRICT,

    INDEX idx_product_name (product_name),
    INDEX idx_product_category_id_status (category_id, status),
    INDEX idx_product_price (unit_price)
) ENGINE = InnoDB COMMENT = '商品信息表';

-- 样例商品编码具有唯一约束，重复执行时 INSERT IGNORE 跳过相同编码。
-- 不会恢复已逻辑删除的同编码商品；执行后应核对实际数据。
INSERT IGNORE INTO product
    (product_code, product_name, category_id, brand_id, unit, unit_price,
     stock_quantity, min_stock, status, production_date, description)
VALUES
    ('DIGI-KEY-001', '机械键盘 K87',
     (SELECT id FROM product_category WHERE category_name = '数码配件'),
     (SELECT id FROM product_brand WHERE brand_name = 'KeyMaster'), '个',
     299.00, 48, 10, 1, '2026-03-15', '87键有线机械键盘'),
    ('DIGI-MOU-002', '无线办公鼠标 M20',
     (SELECT id FROM product_category WHERE category_name = '数码配件'),
     (SELECT id FROM product_brand WHERE brand_name = 'MoveTech'), '个',
     89.90, 6, 15, 1, '2026-04-02', '静音按键，库存低于安全值'),
    ('OFFI-PEN-003', '黑色中性笔 0.5mm',
     (SELECT id FROM product_category WHERE category_name = '办公用品'),
     (SELECT id FROM product_brand WHERE brand_name = '文星'), '盒',
     24.50, 120, 30, 1, '2026-01-10', '每盒12支'),
    ('OFFI-NOT-004', 'A5硬面笔记本',
     (SELECT id FROM product_category WHERE category_name = '办公用品'),
     (SELECT id FROM product_brand WHERE brand_name = '纸间'), '本',
     18.00, 75, 20, 1, '2026-02-18', '共160页'),
    ('HOME-CUP-005', '不锈钢保温杯 500ml',
     (SELECT id FROM product_category WHERE category_name = '生活用品'),
     (SELECT id FROM product_brand WHERE brand_name = 'WarmDay'), '个',
     129.00, 32, 8, 1, '2026-05-06', '食品接触级内胆'),
    ('HOME-LAM-006', 'LED护眼台灯',
     (SELECT id FROM product_category WHERE category_name = '生活用品'),
     (SELECT id FROM product_brand WHERE brand_name = 'BrightHome'), '台',
     159.00, 0, 10, 2, '2026-03-28', '三档色温，当前缺货'),
    ('BOOK-JAV-007', 'Java框架技术实践教程',
     (SELECT id FROM product_category WHERE category_name = '图书'),
     (SELECT id FROM product_brand WHERE brand_name = '启航教育'), '本',
     68.00, 45, 12, 1, '2025-11-01', 'MyBatis与SSM实践参考书'),
    ('FOOD-MIL-008', '纯牛奶 250ml×12',
     (SELECT id FROM product_category WHERE category_name = '食品饮料'),
     (SELECT id FROM product_brand WHERE brand_name = '牧原优选'), '箱',
     59.90, 26, 10, 1, '2026-08-20', '常温保存'),
    ('FOOD-TEA-009', '茉莉花茶 500ml',
     (SELECT id FROM product_category WHERE category_name = '食品饮料'),
     (SELECT id FROM product_brand WHERE brand_name = '清泉'), '瓶',
     5.50, 0, 24, 2, '2026-08-25', '整箱规格为15瓶'),
    ('DIGI-HUB-010', 'USB-C八合一扩展坞',
     (SELECT id FROM product_category WHERE category_name = '数码配件'),
     (SELECT id FROM product_brand WHERE brand_name = 'LinkPro'), '个',
     239.00, 18, 6, 1, '2026-06-12', '支持HDMI与千兆网口'),
    ('OFFI-CHR-011', '人体工学办公椅',
     (SELECT id FROM product_category WHERE category_name = '办公用品'),
     (SELECT id FROM product_brand WHERE brand_name = 'SitWell'), '把',
     899.00, 4, 5, 1, '2025-12-05', '低库存大件商品'),
    ('DIGI-CAM-012', 'USB高清摄像头',
     (SELECT id FROM product_category WHERE category_name = '数码配件'),
     (SELECT id FROM product_brand WHERE brand_name = 'VisionGo'), '个',
     199.00, 12, 6, 0, '2024-09-16', '旧型号，已停止销售');
