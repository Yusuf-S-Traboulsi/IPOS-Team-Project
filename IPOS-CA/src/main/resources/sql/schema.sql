-- ============================================================
-- IPOS-CA Pharmacy Management System - Database Schema
-- ============================================================
-- This script creates all tables for both IPOS-CA (Pharmacy)
-- and IPOS-SA (Supplier) systems.
--
-- Usage:
--   1. Create database: CREATE DATABASE ipos_ca;
--   2. Run this script: mysql -u root -p ipos_ca < schema.sql
--   3. Verify: SHOW TABLES;
-- ============================================================

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ============================================================
-- CREATE DATABASE
-- ============================================================
CREATE DATABASE IF NOT EXISTS ipos_ca;
USE ipos_ca;

-- ============================================================
-- IPOS-CA TABLES (Pharmacy Management System)
-- ============================================================

-- -------------------------------------------------------------
-- Table: users
-- Purpose: System authentication and role-based access control
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
                                     id INT PRIMARY KEY AUTO_INCREMENT,
                                     username VARCHAR(50) UNIQUE NOT NULL,
                                     password VARCHAR(255) NOT NULL,
                                     full_name VARCHAR(100),
                                     role ENUM('ADMIN', 'MANAGER', 'PHARMACIST') NOT NULL,
                                     active TINYINT(1) DEFAULT 1,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     last_login TIMESTAMP NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------
-- Table: customers
-- Purpose: Pharmacy customer accounts with credit management
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS customers (
                                         id INT PRIMARY KEY AUTO_INCREMENT,
                                         title VARCHAR(10),
                                         name VARCHAR(100) NOT NULL,
                                         email VARCHAR(255),
                                         address VARCHAR(255),
                                         town VARCHAR(100),
                                         postcode VARCHAR(20),
                                         credit_limit DECIMAL(10,2) DEFAULT 0.00,
                                         current_debt DECIMAL(10,2) DEFAULT 0.00,
                                         account_status VARCHAR(20) DEFAULT 'Normal',
                                         status_1st_reminder VARCHAR(20) DEFAULT 'no_need',
                                         status_2nd_reminder VARCHAR(20) DEFAULT 'no_need',
                                         date_1st_reminder DATE NULL,
                                         date_2nd_reminder DATE NULL,
                                         oldest_debt_date DATE NULL,
                                         discount_plan_type VARCHAR(20) DEFAULT 'NONE',
                                         discount_rate DECIMAL(5,4) DEFAULT 0.0000,
                                         monthly_purchase_total DECIMAL(10,2) DEFAULT 0.00,
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------
-- Table: products
-- Purpose: Pharmacy inventory and product catalog
-- Updated when supplier orders are delivered
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
                                        id INT PRIMARY KEY,
                                        name VARCHAR(255) NOT NULL,
                                        bulk_cost DECIMAL(10,2) NOT NULL,
                                        markup_rate DECIMAL(5,4) NOT NULL,
                                        price DECIMAL(10,2) NOT NULL,
                                        stock INT NOT NULL DEFAULT 0,
                                        low_stock_threshold INT NOT NULL DEFAULT 20,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------
-- Table: stock_changes
-- Purpose: Audit trail for all stock movements
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_changes (
                                             id INT PRIMARY KEY AUTO_INCREMENT,
                                             product_id INT NOT NULL,
                                             change_amount INT NOT NULL,
                                             reason VARCHAR(100),
                                             user_id INT,
                                             change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                             FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------
-- Table: merchant_settings
-- Purpose: Pharmacy identity and document templates
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS merchant_settings (
                                                 id INT PRIMARY KEY AUTO_INCREMENT,
                                                 company_name VARCHAR(255) NOT NULL,
                                                 address_line1 VARCHAR(255),
                                                 address_line2 VARCHAR(255),
                                                 city VARCHAR(100),
                                                 postcode VARCHAR(20),
                                                 phone VARCHAR(20),
                                                 fax VARCHAR(20),
                                                 email VARCHAR(255),
                                                 website VARCHAR(255),
                                                 logo_path VARCHAR(500),
                                                 registration_number VARCHAR(50),
                                                 vat_number VARCHAR(50),
                                                 director_name VARCHAR(100) DEFAULT 'A. Petite',
                                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- -------------------------------------------------------------
-- Table: document_templates
-- Purpose: Customizable invoice and reminder templates
-- -------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_templates (
                                                  id INT PRIMARY KEY AUTO_INCREMENT,
                                                  template_name VARCHAR(100) NOT NULL UNIQUE,
                                                  template_type ENUM('INVOICE', 'MONTHLY_STATEMENT', 'FIRST_REMINDER', 'SECOND_REMINDER') NOT NULL,
                                                  subject_template TEXT,
                                                  body_template TEXT,
                                                  footer_template TEXT,
                                                  is_active BOOLEAN DEFAULT TRUE,
                                                  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- IPOS-SA TABLES (Supplier Ordering System)
-- ============================================================

-- -------------------------------------------------------------
-- Table: supplier_catalogue
-- Purpose: IPOS-SA product catalogue (InfoPharma wholesale)
-- -------------------------------------------------------------
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

-- -------------------------------------------------------------
-- Table: supplier_orders
-- Purpose: Orders placed by pharmacy to IPOS-SA supplier
-- -------------------------------------------------------------
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

-- -------------------------------------------------------------
-- Table: supplier_order_items
-- Purpose: Individual items within supplier orders
-- -------------------------------------------------------------
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

-- -------------------------------------------------------------
-- Table: supplier_invoices
-- Purpose: Invoices from IPOS-SA to pharmacy
-- -------------------------------------------------------------
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

-- -------------------------------------------------------------
-- Table: ipos_sa_users
-- Purpose: IPOS-SA supplier portal authentication
-- -------------------------------------------------------------
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
-- SEED DATA - IPOS-CA (Pharmacy System)
-- ============================================================

-- Default system users
INSERT INTO users (username, password, full_name, role, active) VALUES
                                                                    ('admin', 'admin123', 'System Administrator', 'ADMIN', 1),
                                                                    ('manager', 'mgr123', 'Pharmacy Manager', 'MANAGER', 1),
                                                                    ('pharmacist', 'pharm123', 'Staff Pharmacist', 'PHARMACIST', 1)
ON DUPLICATE KEY UPDATE username=VALUES(username);

-- Default merchant settings
INSERT INTO merchant_settings (company_name, address_line1, city, postcode, phone, fax, email, website, director_name) VALUES
    ('InfoPharma Ltd.', '19 High St.', 'Ashford', 'Kent', '0208 778 0124', '0208 778 0125', 'accounts@infopharma.co.uk', 'www.infopharma.co.uk', 'A. Petite')
ON DUPLICATE KEY UPDATE company_name=VALUES(company_name);

-- Default document templates
INSERT INTO document_templates (template_name, template_type, subject_template, body_template, footer_template) VALUES
                                                                                                                    ('Default Invoice', 'INVOICE',
                                                                                                                     'Invoice No. {invoice_number}',
                                                                                                                     'Dear {customer_title} {customer_last_name},\n\nThank you for your valued custom.\n\nInvoice Number: {invoice_number}\nTotal Amount Due: £{total_amount}',
                                                                                                                     'Thank you for your valued custom.\n\nYours sincerely,\n\n{director_name}\nDirector of Operations\n{company_name}'),
                                                                                                                    ('Default First Reminder', 'FIRST_REMINDER',
                                                                                                                     'First Payment Reminder - Invoice {invoice_number}',
                                                                                                                     'Dear {customer_title} {customer_last_name},\n\nAccording to our records, payment of £{total_amount} is now due.\n\nPlease settle this invoice at your earliest convenience.',
                                                                                                                     'We value your business.\n\nYours sincerely,\n\n{director_name}\n{company_name}')
ON DUPLICATE KEY UPDATE template_name=VALUES(template_name);

-- Default products (core pharmacy inventory)
INSERT INTO products (id, name, bulk_cost, markup_rate, price, stock, low_stock_threshold) VALUES
                                                                                               (1, 'Paracetamol', 2.00, 0.25, 3.00, 100, 20),
                                                                                               (2, 'Ibuprofen', 1.50, 0.30, 2.34, 150, 25),
                                                                                               (3, 'Aspirin', 1.00, 0.35, 1.62, 200, 30),
                                                                                               (4, 'Vitamin C', 3.00, 0.20, 4.32, 75, 15),
                                                                                               (5, 'Cough Syrup', 4.50, 0.25, 6.75, 50, 10)
ON DUPLICATE KEY UPDATE name=VALUES(name);

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

-- IPOS-SA portal users (for supplier login simulation)
INSERT INTO ipos_sa_users (username, password, company_name, account_number, active) VALUES
                                                                                         ('supplier', 'supplier123', 'Cosymed Ltd.', '0000235', 1),
                                                                                         ('merchant', 'merchant123', 'Test Pharmacy', '0000236', 1)
ON DUPLICATE KEY UPDATE username=VALUES(username);

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