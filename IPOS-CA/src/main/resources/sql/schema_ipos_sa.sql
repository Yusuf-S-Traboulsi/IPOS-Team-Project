-- ============================================================
-- IPOS-SA Supplier Ordering System - Database Schema
-- ============================================================
-- This script creates all tables for IPOS-SA (Supplier) system.
-- Usage:
--   1. Create database: CREATE DATABASE ipos_sa;
--   2. Run this script: mysql -u root -p ipos_sa < schema_ipos_sa.sql
--   3. Verify: SHOW TABLES;
-- ============================================================

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ============================================================
-- CREATE DATABASE
-- ============================================================
CREATE DATABASE IF NOT EXISTS ipos_sa;
USE ipos_sa;

-- ============================================================
-- IPOS-SA TABLES (Supplier Ordering System)
-- ============================================================

-- Table: supplier_catalogue
-- Purpose: IPOS-SA product catalogue (InfoPharma wholesale)
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
-- Purpose: Orders placed by pharmacy to IPOS-SA supplier
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
-- Purpose: Individual items within supplier orders
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
-- Purpose: Invoices from IPOS-SA to pharmacy
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
-- Purpose: IPOS-SA supplier portal authentication
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
-- SEED DATA - IPOS-SA (Supplier System)
-- ============================================================

-- IPOS-SA supplier catalogue (Section 9.1 from briefing)
INSERT INTO supplier_catalogue (item_id, description, package_type, unit, units_per_pack, package_cost, availability, stock_limit, category) VALUES
-- Category 100: Pain Relief
('100 00001', 'Paracetamol', 'box', 'Caps', 20, 2.00, 10345, 300, 'Pain Relief'),
('100 00002', 'Ibuprofen', 'box', 'Caps', 20, 1.50, 12453, 500, 'Pain Relief'),
('100 00003', 'Aspirin', 'box', 'Caps', 10, 1.00, 4235, 200, 'Pain Relief'),
('100 00004', 'Celebrex, caps 100 mg', 'box', 'Caps', 10, 10.00, 3420, 200, 'Pain Relief'),
('100 00005', 'Celebrex, caps 200 mg', 'box', 'caps', 10, 18.50, 1450, 150, 'Pain Relief'),
('100 00006', 'Retin-A Tretin, 30 g', 'box', 'caps', 20, 25.00, 2013, 200, 'Pain Relief'),
('100 00007', 'Lipitor TB, 20 mg', 'box', 'caps', 30, 15.50, 1562, 200, 'Pain Relief'),
('100 00008', 'Claritin CR, 60g', 'box', 'caps', 20, 19.50, 2540, 200, 'Pain Relief'),
-- Category 200: Antiseptics & Cold Relief
('200 00004', 'Iodine tincture', 'bottle', 'ml', 100, 0.30, 22134, 200, 'Antiseptics'),
('200 00005', 'Rhynol', 'bottle', 'ml', 200, 2.50, 1908, 300, 'Cold Relief'),
-- Category 300: Antibiotics
('300 00001', 'Ospen', 'box', 'caps', 20, 10.50, 809, 200, 'Antibiotics'),
('300 00002', 'Amopen', 'box', 'caps', 30, 15.00, 1340, 300, 'Antibiotics'),
-- Category 400: Vitamins
('400 00001', 'Vitamin C', 'box', 'caps', 30, 3.00, 3258, 300, 'Vitamins'),
('400 00002', 'Vitamin B12', 'box', 'caps', 30, 1.30, 2673, 300, 'Vitamins')
    ON DUPLICATE KEY UPDATE description=VALUES(description);

-- IPOS-SA portal users
-- NOTE: Default users are no longer seeded here to avoid storing passwords in plaintext.
-- Create application users via a secure bootstrap process that hashes passwords.

-- Sample supplier orders (for testing order tracking)
INSERT INTO supplier_orders (order_id, order_date, total_amount, status, dispatched_date, delivered_date, paid_date, payment_status) VALUES
                                                                                                                                         ('IP2034', '2026-01-12', 302.50, 'Delivered', '2026-01-14', '2026-01-15', '2026-01-20', 'Paid'),
                                                                                                                                         ('IP2780', '2026-01-17', 525.00, 'Delivered', '2026-01-18', '2026-01-19', NULL, 'Pending'),
                                                                                                                                         ('IP3021', '2026-01-29', 750.30, 'Ordered', NULL, NULL, NULL, 'Pending')
    ON DUPLICATE KEY UPDATE order_id=VALUES(order_id);

-- Sample order items
INSERT INTO supplier_order_items (order_id, item_id, description, quantity, unit_cost, amount) VALUES
                                                                                                   ('IP2034', '100 00001', 'Paracetamol', 10, 2.00, 20.00),
                                                                                                   ('IP2034', '100 00003', 'Aspirin', 20, 1.00, 20.00),
                                                                                                   ('IP2034', '300 00001', 'Ospen', 10, 10.50, 105.00),
                                                                                                   ('IP2034', '300 00002', 'Amopen', 20, 15.00, 300.00),
                                                                                                   ('IP2780', '200 00004', 'Iodine tincture', 50, 0.30, 15.00),
                                                                                                   ('IP2780', '200 00005', 'Rhynol', 100, 2.50, 250.00),
                                                                                                   ('IP2780', '400 00001', 'Vitamin C', 100, 3.00, 300.00),
                                                                                                   ('IP3021', '100 00004', 'Celebrex, caps 100 mg', 30, 10.00, 300.00),
                                                                                                   ('IP3021', '100 00005', 'Celebrex, caps 200 mg', 20, 18.50, 370.00)
    ON DUPLICATE KEY UPDATE order_id=VALUES(order_id);

-- Sample invoices
INSERT INTO supplier_invoices (invoice_id, order_id, invoice_date, amount, paid_amount, outstanding_balance, due_date, status) VALUES
                                                                                                                                   ('INV-2034', 'IP2034', '2026-01-15', 302.50, 302.50, 0.00, '2026-02-15', 'Paid'),
                                                                                                                                   ('INV-2780', 'IP2780', '2026-01-19', 525.00, 0.00, 525.00, '2026-02-19', 'Unpaid'),
                                                                                                                                   ('INV-3021', 'IP3021', '2026-01-29', 750.30, 0.00, 750.30, '2026-02-28', 'Unpaid')
    ON DUPLICATE KEY UPDATE invoice_id=VALUES(invoice_id);

-- ============================================================
-- RESTORE SETTINGS
-- ============================================================
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- ============================================================
-- END OF SCHEMA
-- ============================================================