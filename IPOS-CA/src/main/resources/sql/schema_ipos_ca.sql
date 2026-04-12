-- ============================================================
-- IPOS-CA Database Schema - Compatible with Sample Data
-- ============================================================

DROP DATABASE IF EXISTS `ipos_ca`;
CREATE DATABASE `ipos_ca` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE `ipos_ca`;

-- ============================================================
-- TABLE: users (USER Package - 6 marks)
-- ============================================================
CREATE TABLE `users` (
                         `id` int NOT NULL AUTO_INCREMENT,
                         `username` varchar(50) NOT NULL,
                         `full_name` varchar(255) NOT NULL,
                         `password` varchar(255) NOT NULL,
                         `role` enum('ADMIN','MANAGER','PHARMACIST') NOT NULL,
                         `active` tinyint(1) DEFAULT '1',
                         PRIMARY KEY (`id`),
                         UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed data for users (from Sample Data PDF)
INSERT INTO `users` (`username`, `full_name`, `password`, `role`, `active`) VALUES
                                                                                ('sysdba', 'System Administrator', 'masterkey', 'ADMIN', 1),
                                                                                ('manager', 'Mike Manager', 'Get_it_done', 'MANAGER', 1),
                                                                                ('accountant', 'Senior Accountant', 'Count_money', 'MANAGER', 1),
                                                                                ('clerk', 'Account Clerk', 'Paperwork', 'PHARMACIST', 1);

-- ============================================================
-- TABLE: customers (CUST Package - 31 marks)
-- ============================================================
CREATE TABLE `customers` (
                             `id` int NOT NULL,
                             `title` varchar(10) DEFAULT NULL,
                             `name` varchar(255) NOT NULL,
                             `email` varchar(255) DEFAULT NULL,
                             `address` varchar(500) DEFAULT NULL,
                             `town` varchar(100) DEFAULT NULL,
                             `postcode` varchar(20) DEFAULT NULL,
                             `credit_limit` decimal(10,2) DEFAULT '0.00',
                             `current_debt` decimal(10,2) DEFAULT '0.00',
                             `account_status` varchar(20) DEFAULT 'Normal',
                             `discount_plan_type` varchar(20) DEFAULT 'NONE',
                             `discount_rate` decimal(5,4) DEFAULT '0.0000',
                             `monthly_purchase_total` decimal(10,2) DEFAULT '0.00',
                             `status_1st_reminder` varchar(20) DEFAULT 'no_need',
                             `status_2nd_reminder` varchar(20) DEFAULT 'no_need',
                             `date_1st_reminder` date DEFAULT NULL,
                             `date_2nd_reminder` date DEFAULT NULL,
                             `oldest_debt_date` date DEFAULT NULL,
                             PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed data for customers (from Sample Data PDF - Cosymed Ltd customers)
INSERT INTO `customers` (`id`, `title`, `name`, `email`, `address`, `town`, `postcode`, `credit_limit`, `current_debt`, `account_status`, `discount_plan_type`, `discount_rate`, `monthly_purchase_total`, `oldest_debt_date`) VALUES
                                                                                                                                                                                                                                   (101, 'Ms', 'Eva Bauyer', 'eva@email.com', '1, Liverpool street', 'London', 'EC2V 8NS', 500.00, 0.00, 'Normal', 'FIXED', 0.0300, 0.00, NULL),
                                                                                                                                                                                                                                   (102, 'Mr', 'Glynne Morrison', 'glynne@email.com', '1, Liverpool street', 'London', 'EC2V 8NS', 500.00, 0.00, 'Normal', 'VARIABLE', 0.0000, 0.00, NULL),
                                                                                                                                                                                                                                   (103, 'Dr', 'Bob Johnson', 'bob@email.com', '789 Pine Ln', 'Birmingham', 'B1 1AA', 750.00, 0.00, 'Normal', 'NONE', 0.0000, 0.00, NULL);

-- ============================================================
-- TABLE: products (STOCK Package - 8 marks)
-- ============================================================
CREATE TABLE `products` (
                            `id` int NOT NULL,
                            `supplier_item_id` varchar(50) DEFAULT NULL,
                            `name` varchar(255) NOT NULL,
                            `bulk_cost` decimal(10,2) NOT NULL,
                            `markup_rate` decimal(5,2) NOT NULL,
                            `price` decimal(10,2) NOT NULL,
                            `stock` int NOT NULL,
                            `low_stock_threshold` int NOT NULL,
                            PRIMARY KEY (`id`),
                            KEY `idx_supplier_item_id` (`supplier_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed data for products (from Sample Data PDF - Cosymed Ltd stock)
-- Prices calculated with 100% markup + 20% VAT as per briefing
INSERT INTO `products` (`id`, `supplier_item_id`, `name`, `bulk_cost`, `markup_rate`, `price`, `stock`, `low_stock_threshold`) VALUES
                                                                                                                                   (1, '100 00001', 'Paracetamol', 2.00, 0.25, 3.00, 121, 10),
                                                                                                                                   (2, '100 00002', 'Aspirin', 1.00, 0.35, 1.62, 201, 15),
                                                                                                                                   (3, '100 00003', 'Analgin', 1.20, 0.35, 1.94, 25, 10),
                                                                                                                                   (4, '100 00004', 'Celebrex, caps 100 mg', 10.00, 0.35, 16.20, 43, 10),
                                                                                                                                   (5, '100 00005', 'Celebrex, caps 200 mg', 18.50, 0.35, 29.97, 35, 5),
                                                                                                                                   (6, '100 00006', 'Retin-A Tretin, 30 g', 25.00, 0.35, 40.50, 28, 10),
                                                                                                                                   (7, '100 00007', 'Lipitor TB, 20 mg', 15.50, 0.35, 25.11, 10, 10),
                                                                                                                                   (8, '100 00008', 'Claritin CR, 60g', 19.50, 0.35, 31.59, 21, 10),
                                                                                                                                   (9, '200 00004', 'Iodine tincture', 0.30, 0.35, 0.49, 35, 10),
                                                                                                                                   (10, '200 00005', 'Rhynol', 2.50, 0.35, 4.05, 14, 15),
                                                                                                                                   (11, '300 00001', 'Ospen', 10.50, 0.35, 17.01, 78, 10),
                                                                                                                                   (12, '300 00002', 'Amopen', 15.00, 0.35, 24.30, 90, 15),
                                                                                                                                   (13, '400 00001', 'Vitamin C', 1.20, 0.35, 1.94, 22, 15),
                                                                                                                                   (14, '400 00002', 'Vitamin B12', 1.30, 0.35, 2.11, 43, 15);

-- ============================================================
-- TABLE: sales (SALES Package - 8 marks)
-- ============================================================
CREATE TABLE `sales` (
                         `id` int NOT NULL,
                         `customer_id` int DEFAULT NULL,
                         `customer_name` varchar(255) NOT NULL,
                         `is_account_holder` tinyint(1) DEFAULT '0',
                         `total_before_discount` decimal(10,2) NOT NULL,
                         `discount_rate` decimal(5,4) DEFAULT '0.0000',
                         `discount_amount` decimal(10,2) DEFAULT '0.00',
                         `total_after_discount` decimal(10,2) NOT NULL,
                         `vat_amount` decimal(10,2) NOT NULL,
                         `total_with_vat` decimal(10,2) NOT NULL,
                         `payment_type` varchar(20) NOT NULL,
                         `sale_date` date NOT NULL,
                         PRIMARY KEY (`id`),
                         KEY `customer_id` (`customer_id`),
                         CONSTRAINT `sales_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed sales data (from Sample Data PDF - March 2026 transactions)
-- Scenario 10: Eva Bauyer - 1 March 2026
INSERT INTO `sales` (`id`, `customer_id`, `customer_name`, `is_account_holder`, `total_before_discount`, `discount_rate`, `discount_amount`, `total_after_discount`, `vat_amount`, `total_with_vat`, `payment_type`, `sale_date`) VALUES
    (50001, 101, 'Eva Bauyer', 1, 63.33, 0.0300, 1.90, 61.43, 12.29, 73.72, 'CARD', '2026-03-01');

-- Scenario 11: Cash customers - 3 March 2026
INSERT INTO `sales` (`id`, `customer_id`, `customer_name`, `is_account_holder`, `total_before_discount`, `discount_rate`, `discount_amount`, `total_after_discount`, `vat_amount`, `total_with_vat`, `payment_type`, `sale_date`) VALUES
                                                                                                                                                                                                                                      (50002, NULL, 'Walk-in Customer', 0, 4.86, 0.0000, 0.00, 4.86, 0.97, 5.83, 'CASH', '2026-03-03'),
                                                                                                                                                                                                                                      (50003, NULL, 'Walk-in Customer', 0, 113.40, 0.0000, 0.00, 113.40, 22.68, 136.08, 'CARD', '2026-03-03'),
                                                                                                                                                                                                                                      (50004, NULL, 'Walk-in Customer', 0, 56.70, 0.0000, 0.00, 56.70, 11.34, 68.04, 'CASH', '2026-03-03'),
                                                                                                                                                                                                                                      (50005, NULL, 'Walk-in Customer', 0, 48.60, 0.0000, 0.00, 48.60, 9.72, 58.32, 'CASH', '2026-03-03'),
                                                                                                                                                                                                                                      (50006, NULL, 'Walk-in Customer', 0, 7.78, 0.0000, 0.00, 7.78, 1.56, 9.34, 'CARD', '2026-03-03'),
                                                                                                                                                                                                                                      (50007, NULL, 'Walk-in Customer', 0, 52.92, 0.0000, 0.00, 52.92, 10.58, 63.50, 'CASH', '2026-03-03');

-- Scenario 12: Glynne Morrison (Account Holder) - 5 March 2026
INSERT INTO `sales` (`id`, `customer_id`, `customer_name`, `is_account_holder`, `total_before_discount`, `discount_rate`, `discount_amount`, `total_after_discount`, `vat_amount`, `total_with_vat`, `payment_type`, `sale_date`) VALUES
    (50008, 102, 'Glynne Morrison', 1, 129.60, 0.0200, 2.59, 127.01, 25.40, 152.41, 'ACCOUNT', '2026-03-05');

-- Scenario 13: Eva Bauyer - 1 April 2026
INSERT INTO `sales` (`id`, `customer_id`, `customer_name`, `is_account_holder`, `total_before_discount`, `discount_rate`, `discount_amount`, `total_after_discount`, `vat_amount`, `total_with_vat`, `payment_type`, `sale_date`) VALUES
    (50009, 101, 'Eva Bauyer', 1, 93.96, 0.0300, 2.82, 91.14, 18.23, 109.37, 'ACCOUNT', '2026-04-01');

-- ============================================================
-- TABLE: stock_changes (STOCK Package - Audit Trail)
-- ============================================================
CREATE TABLE `stock_changes` (
                                 `id` int NOT NULL AUTO_INCREMENT,
                                 `product_id` int DEFAULT NULL,
                                 `change_amount` int NOT NULL,
                                 `change_date` datetime DEFAULT CURRENT_TIMESTAMP,
                                 `reason` varchar(100) DEFAULT NULL,
                                 `user_id` int DEFAULT NULL,
                                 PRIMARY KEY (`id`),
                                 KEY `product_id` (`product_id`),
                                 CONSTRAINT `stock_changes_ibfk_1` FOREIGN KEY (`product_id`) REFERENCES `products` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE: merchant_settings (TEMPLATES Package - 3 marks)
-- ============================================================
CREATE TABLE `merchant_settings` (
                                     `id` int NOT NULL AUTO_INCREMENT,
                                     `company_name` varchar(255) NOT NULL,
                                     `address_line1` varchar(255) DEFAULT NULL,
                                     `address_line2` varchar(255) DEFAULT NULL,
                                     `city` varchar(100) DEFAULT NULL,
                                     `postcode` varchar(20) DEFAULT NULL,
                                     `phone` varchar(20) DEFAULT NULL,
                                     `fax` varchar(20) DEFAULT NULL,
                                     `email` varchar(255) DEFAULT NULL,
                                     `website` varchar(255) DEFAULT NULL,
                                     `logo_path` varchar(500) DEFAULT NULL,
                                     `registration_number` varchar(50) DEFAULT NULL,
                                     `vat_number` varchar(50) DEFAULT NULL,
                                     `director_name` varchar(100) DEFAULT 'A. Petite',
                                     `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     `vat_rate` decimal(5,4) DEFAULT '0.2000',
                                     PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `merchant_settings` (`id`, `company_name`, `address_line1`, `address_line2`, `city`, `postcode`, `phone`, `fax`, `email`, `director_name`, `vat_rate`) VALUES
    (1, 'Cosymed Ltd.', '3, High Level Drive', '', 'Sydenham', 'SE26 3ET', '0208 778 0124', '0208 778 0125', 'accounts@cosymed.co.uk', 'A. Petite', 0.2000);

-- ============================================================
-- TABLE: document_templates (TEMPLATES Package - 3 marks)
-- ============================================================
CREATE TABLE `document_templates` (
                                      `id` int NOT NULL AUTO_INCREMENT,
                                      `template_name` varchar(100) NOT NULL,
                                      `template_type` enum('INVOICE','MONTHLY_STATEMENT','FIRST_REMINDER','SECOND_REMINDER') NOT NULL,
                                      `subject_template` text,
                                      `body_template` text,
                                      `footer_template` text,
                                      `is_active` tinyint(1) DEFAULT '1',
                                      `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `template_name` (`template_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Seed templates (from Sample Data PDF)
INSERT INTO `document_templates` (`template_name`, `template_type`, `subject_template`, `body_template`, `footer_template`, `is_active`) VALUES
                                                                                                                                             ('Default Invoice', 'INVOICE', 'Invoice No. {invoice_number}', 'Dear {customer_title} {customer_last_name},\n\nThank you for your valued custom.\n\nInvoice Number: {invoice_number}\nTotal Amount Due: £{total_amount}', 'Thank you for your valued custom.\n\nYours sincerely,\n\n{director_name}\nDirector of Operations\n{company_name}', 1),
                                                                                                                                             ('Default Monthly Statement', 'MONTHLY_STATEMENT', 'Monthly Statement - {month} {year}', 'Dear {customer_title} {customer_last_name},\n\nPlease find your monthly statement attached.\n\nTotal Outstanding: £{total_outstanding}', 'Thank you for your continued custom.\n\nYours sincerely,\n\n{director_name}\n{company_name}', 1),
                                                                                                                                             ('Default First Reminder', 'FIRST_REMINDER', 'First Payment Reminder - Invoice {invoice_number}', 'Dear {customer_title} {customer_last_name},\n\nAccording to our records, payment of £{total_amount} is now due.\n\nPlease settle this invoice at your earliest convenience.', 'We value your business.\n\nYours sincerely,\n\n{director_name}\n{company_name}', 1),
                                                                                                                                             ('Default Second Reminder', 'SECOND_REMINDER', 'Second Payment Reminder - Invoice {invoice_number}', 'Dear {customer_title} {customer_last_name},\n\nThis is our second reminder regarding the outstanding payment of £{total_amount}.\n\nPlease arrange payment immediately to avoid further action.', 'We value your business.\n\nYours sincerely,\n\n{director_name}\n{company_name}', 1);

-- ============================================================
-- TABLE: supplier_orders (ORD Package - 6 marks)
-- ============================================================
CREATE TABLE `supplier_orders` (
                                   `order_id` varchar(20) NOT NULL,
                                   `order_date` date NOT NULL,
                                   `total_amount` decimal(10,2) NOT NULL,
                                   `status` varchar(20) DEFAULT 'Ordered',
                                   `dispatched_date` date DEFAULT NULL,
                                   `delivered_date` date DEFAULT NULL,
                                   `paid_date` date DEFAULT NULL,
                                   `payment_status` varchar(20) DEFAULT 'Pending',
                                   `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE: supplier_order_items (ORD Package)
-- ============================================================
CREATE TABLE `supplier_order_items` (
                                        `id` int NOT NULL AUTO_INCREMENT,
                                        `order_id` varchar(20) DEFAULT NULL,
                                        `item_id` varchar(20) DEFAULT NULL,
                                        `description` varchar(255) DEFAULT NULL,
                                        `quantity` int NOT NULL,
                                        `unit_cost` decimal(10,2) NOT NULL,
                                        `amount` decimal(10,2) NOT NULL,
                                        PRIMARY KEY (`id`),
                                        KEY `order_id` (`order_id`),
                                        KEY `item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- TABLE: supplier_invoices (ORD Package)
-- ============================================================
CREATE TABLE `supplier_invoices` (
                                     `invoice_id` varchar(20) NOT NULL,
                                     `order_id` varchar(20) DEFAULT NULL,
                                     `invoice_date` date DEFAULT NULL,
                                     `amount` decimal(10,2) NOT NULL,
                                     `paid_amount` decimal(10,2) DEFAULT '0.00',
                                     `outstanding_balance` decimal(10,2) NOT NULL,
                                     `due_date` date DEFAULT NULL,
                                     `status` varchar(20) DEFAULT 'Unpaid',
                                     PRIMARY KEY (`invoice_id`),
                                     KEY `order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- VERIFICATION QUERIES
-- ============================================================

-- Verify all tables created
SELECT 'Tables created successfully!' as Status;

-- Verify user accounts
SELECT username, role, active FROM users;

-- Verify customers
SELECT id, name, credit_limit, account_status, discount_plan_type FROM customers;

-- Verify products
SELECT id, name, stock, low_stock_threshold, price FROM products ORDER BY id;

-- Verify merchant settings
SELECT company_name, director_name, vat_rate FROM merchant_settings;

-- Verify templates
SELECT template_name, template_type, is_active FROM document_templates;