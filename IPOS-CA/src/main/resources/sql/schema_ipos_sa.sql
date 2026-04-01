-- ============================================================
-- IPOS-SA Supplier Ordering System - Database Schema
-- ============================================================
CREATE DATABASE IF NOT EXISTS ipos_sa;
USE ipos_sa;

-- Table: supplier_catalogue
CREATE TABLE IF NOT EXISTS supplier_catalogue (
                                                  item_id VARCHAR(20) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    package_type VARCHAR(50),
    unit VARCHAR(20),
    units_per_pack INT,
    package_cost DECIMAL(10,2) NOT NULL,
    availability INT DEFAULT 0,
    stock_limit INT DEFAULT 0,
    category VARCHAR(50),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: supplier_orders
CREATE TABLE IF NOT EXISTS supplier_orders (
                                               order_id VARCHAR(20) PRIMARY KEY,
    order_date DATE NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'Ordered',
    dispatched_date DATE NULL,
    delivered_date DATE NULL,
    paid_date DATE NULL,
    payment_status VARCHAR(20) DEFAULT 'Pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: supplier_order_items
CREATE TABLE IF NOT EXISTS supplier_order_items (
                                                    id INT PRIMARY KEY AUTO_INCREMENT,
                                                    order_id VARCHAR(20) NOT NULL,
    item_id VARCHAR(20) NOT NULL,
    description VARCHAR(255),
    quantity INT NOT NULL,
    unit_cost DECIMAL(10,2) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES supplier_orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (item_id) REFERENCES supplier_catalogue(item_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: supplier_invoices
CREATE TABLE IF NOT EXISTS supplier_invoices (
                                                 invoice_id VARCHAR(20) PRIMARY KEY,
    order_id VARCHAR(20) NOT NULL,
    invoice_date DATE,
    amount DECIMAL(10,2) NOT NULL,
    paid_amount DECIMAL(10,2) DEFAULT 0,
    outstanding_balance DECIMAL(10,2) NOT NULL,
    due_date DATE,
    status VARCHAR(20) DEFAULT 'Unpaid',
    FOREIGN KEY (order_id) REFERENCES supplier_orders(order_id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: ipos_sa_users
CREATE TABLE IF NOT EXISTS ipos_sa_users (
                                             id INT PRIMARY KEY AUTO_INCREMENT,
                                             username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    company_name VARCHAR(255),
    account_number VARCHAR(20),
    active TINYINT(1) DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- SEED DATA - IPOS-SA
-- ============================================================
INSERT INTO supplier_catalogue (item_id, description, package_type, unit, units_per_pack, package_cost, availability, stock_limit, category) VALUES
                                                                                                                                                 ('100 00001', 'Paracetamol', 'box', 'Caps', 20, 2.00, 10345, 300, 'Pain Relief'),
                                                                                                                                                 ('100 00002', 'Ibuprofen', 'box', 'Caps', 20, 1.50, 12453, 500, 'Pain Relief'),
                                                                                                                                                 ('100 00003', 'Aspirin', 'box', 'Caps', 10, 1.00, 4235, 200, 'Pain Relief'),
                                                                                                                                                 ('200 00004', 'Iodine tincture', 'bottle', 'ml', 100, 0.30, 22134, 200, 'Antiseptics'),
                                                                                                                                                 ('200 00005', 'Rhynol', 'bottle', 'ml', 200, 2.50, 1908, 300, 'Cold Relief'),
                                                                                                                                                 ('300 00001', 'Ospen', 'box', 'caps', 20, 10.50, 809, 200, 'Antibiotics'),
                                                                                                                                                 ('300 00002', 'Amopen', 'box', 'caps', 30, 15.00, 1340, 300, 'Antibiotics'),
                                                                                                                                                 ('400 00001', 'Vitamin C', 'box', 'caps', 30, 3.00, 3258, 300, 'Vitamins'),
                                                                                                                                                 ('400 00002', 'Vitamin B12', 'box', 'caps', 30, 1.30, 2673, 300, 'Vitamins')
    ON DUPLICATE KEY UPDATE description=VALUES(description);

INSERT INTO ipos_sa_users (username, password, company_name, account_number, active) VALUES
                                                                                         ('supplier', 'supplier123', 'Cosymed Ltd.', '0000235', 1),
                                                                                         ('merchant', 'merchant123', 'Test Pharmacy', '0000236', 1)
    ON DUPLICATE KEY UPDATE username=VALUES(username);

INSERT INTO supplier_orders (order_id, order_date, total_amount, status, dispatched_date, delivered_date, paid_date, payment_status) VALUES
                                                                                                                                         ('IP2034', '2026-01-12', 302.50, 'Delivered', '2026-01-14', '2026-01-15', '2026-01-20', 'Paid'),
                                                                                                                                         ('IP2780', '2026-01-17', 525.00, 'Delivered', '2026-01-18', '2026-01-19', NULL, 'Pending'),
                                                                                                                                         ('IP3021', '2026-01-29', 750.30, 'Ordered', NULL, NULL, NULL, 'Pending')
    ON DUPLICATE KEY UPDATE order_id=VALUES(order_id);