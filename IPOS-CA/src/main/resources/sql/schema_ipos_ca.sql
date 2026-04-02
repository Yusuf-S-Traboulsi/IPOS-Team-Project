-- ============================================================
-- IPOS-CA Pharmacy Management System - Database Schema
-- ============================================================
-- This script creates all tables for IPOS-CA (Pharmacy) system.
-- Usage:
--   1. Create database: CREATE DATABASE ipos_ca;
--   2. Run this script: mysql -u root -p ipos_ca < schema_ipos_ca.sql
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

-- Table: users
-- Purpose: System authentication and role-based access control
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

-- Table: customers
-- Purpose: Pharmacy customer accounts with credit management
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

-- Table: products
-- Purpose: Pharmacy inventory and product catalog
-- Updated when supplier orders are delivered
CREATE TABLE IF NOT EXISTS products (
                                        id INT PRIMARY KEY,
                                        name VARCHAR(255) NOT NULL,
    bulk_cost DECIMAL(10,2) NOT NULL,
    markup_rate DECIMAL(5,4) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    low_stock_threshold INT NOT NULL DEFAULT 20,
    supplier_item_id VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: stock_changes
-- Purpose: Audit trail for all stock movements
CREATE TABLE IF NOT EXISTS stock_changes (
                                             id INT PRIMARY KEY AUTO_INCREMENT,
                                             product_id INT NOT NULL,
                                             change_amount INT NOT NULL,
                                             reason VARCHAR(100),
    user_id INT,
    change_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Table: merchant_settings
-- Purpose: Pharmacy identity and document templates
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

-- Table: document_templates
-- Purpose: Customizable invoice and reminder templates
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
-- SEED DATA - IPOS-CA (Pharmacy System)
-- ============================================================

-- Default system users
-- Note: User passwords are stored as bcrypt hashes, not plaintext.
-- The example hashes below are for demo accounts only and must not be used in production.
INSERT INTO users (username, password, full_name, role, active) VALUES
                                                                    ('admin', '$2b$12$C6UzMDM.H6dfI/f/IKcEe.7sS4uXEt3G5UpPOgoY1oCbcnZ6F.Oa', 'System Administrator', 'ADMIN', 1),
                                                                    ('manager', '$2b$12$7vE3Pjz9G6YxZsV10W7CqO1Zzvvi8YF2cT9C7xR7pQ8o5KxkBYmV6', 'Pharmacy Manager', 'MANAGER', 1),
                                                                    ('pharmacist', '$2b$12$9qH1LmNw5F3aZcX4Yt7JuO0vD3hKfUeS8pQzR1nV2wXyL5bNcDeFa', 'Staff Pharmacist', 'PHARMACIST', 1)
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
INSERT INTO products (id, name, bulk_cost, markup_rate, price, stock, low_stock_threshold, supplier_item_id) VALUES
                                                                                                                 (1, 'Paracetamol', 2.00, 0.25, 3.00, 100, 20, '100 00001'),
                                                                                                                 (2, 'Ibuprofen', 1.50, 0.30, 2.34, 150, 25, '100 00002'),
                                                                                                                 (3, 'Aspirin', 1.00, 0.35, 1.62, 200, 30, '100 00003'),
                                                                                                                 (4, 'Vitamin C', 3.00, 0.20, 4.32, 75, 15, '400 00001'),
                                                                                                                 (5, 'Cough Syrup', 4.50, 0.25, 6.75, 50, 10, NULL)
    ON DUPLICATE KEY UPDATE name=VALUES(name);

-- ============================================================
-- RESTORE SETTINGS
-- ============================================================
SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- ============================================================
-- END OF SCHEMA
-- ============================================================