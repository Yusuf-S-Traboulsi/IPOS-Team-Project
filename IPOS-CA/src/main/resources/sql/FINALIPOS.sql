CREATE DATABASE  IF NOT EXISTS `ipos_sa` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ipos_sa`;
-- MySQL dump 10.13  Distrib 8.0.44, for macos15 (arm64)
--
-- Host: 127.0.0.1    Database: ipos_sa
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'c812522e-261d-11f1-a08b-697e3eb8e349:1-608';

--
-- Table structure for table `ipos_sa_users`
--

DROP TABLE IF EXISTS `ipos_sa_users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ipos_sa_users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `password` varchar(255) NOT NULL,
  `full_name` varchar(255) DEFAULT NULL,
  `role` varchar(50) NOT NULL,
  `company_name` varchar(255) DEFAULT NULL,
  `account_number` varchar(20) DEFAULT NULL,
  `active` tinyint(1) DEFAULT '1',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `last_login` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ipos_sa_users`
--

LOCK TABLES `ipos_sa_users` WRITE;
/*!40000 ALTER TABLE `ipos_sa_users` DISABLE KEYS */;
INSERT INTO `ipos_sa_users` VALUES (1,'sysdba','London_weighting','System Administrator','ADMIN','InfoPharma Ltd.','ADMIN001',1,'2026-04-12 22:26:40',NULL),(2,'manager','Get_it_done','Director of Operations','MANAGER','InfoPharma Ltd.','MGR001',1,'2026-04-12 22:26:40',NULL),(3,'accountant','Count_money','Senior Accountant','ACCOUNTANT','InfoPharma Ltd.','ACC001',1,'2026-04-12 22:26:40',NULL),(4,'clerk','Paperwork','Account Clerk','CLERK','InfoPharma Ltd.','CLK001',1,'2026-04-12 22:26:40',NULL),(5,'warehouse1','Get_a_beer','Warehouse Employee 1','WAREHOUSE','InfoPharma Ltd.','WH001',1,'2026-04-12 22:26:40',NULL),(6,'warehouse2','Lot_smell','Warehouse Employee 2','WAREHOUSE','InfoPharma Ltd.','WH002',1,'2026-04-12 22:26:40',NULL),(7,'delivery','Too_dark','Delivery Department','DELIVERY','InfoPharma Ltd.','DLV001',1,'2026-04-12 22:26:40',NULL),(8,'supplier','supplier123','Cosymed Ltd','MERCHANT','Cosymed Ltd.','0000235',1,'2026-04-12 22:26:40',NULL),(9,'merchant','merchant123','Test Pharmacy','MERCHANT','Test Pharmacy','0000236',1,'2026-04-12 22:26:40',NULL);
/*!40000 ALTER TABLE `ipos_sa_users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchants`
--

DROP TABLE IF EXISTS `merchants`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `merchants` (
  `id` int NOT NULL AUTO_INCREMENT,
  `account_number` varchar(20) NOT NULL,
  `company_name` varchar(255) NOT NULL,
  `contact_name` varchar(255) DEFAULT NULL,
  `address` varchar(500) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL,
  `credit_limit` decimal(10,2) DEFAULT '0.00',
  `current_balance` decimal(10,2) DEFAULT '0.00',
  `discount_type` varchar(20) DEFAULT 'NONE',
  `discount_rate` decimal(5,4) DEFAULT '0.0000',
  `account_status` varchar(20) DEFAULT 'Normal',
  `login_username` varchar(50) DEFAULT NULL,
  `login_password` varchar(255) DEFAULT NULL,
  `monthly_purchase_total` decimal(10,2) DEFAULT '0.00',
  `last_payment_date` date DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `account_number` (`account_number`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchants`
--

LOCK TABLES `merchants` WRITE;
/*!40000 ALTER TABLE `merchants` DISABLE KEYS */;
INSERT INTO `merchants` VALUES (1,'ACC0001','CityPharmacy','Prof David Rhind','Northampton Square, London EC1V 0HB','0207 040 8000',10000.00,0.00,'FIXED',0.0300,'Normal',NULL,NULL,0.00,NULL),(2,'ACC0002','Cosymed Ltd','Mr Alex Wright','25, Bond Street, London WC1V 8LS','0207 321 8001',5000.00,0.00,'VARIABLE',0.0000,'Normal',NULL,NULL,0.00,NULL),(3,'ACC0003','HelloPharmacy','Mr Bruno Wright','12, Bond Street, London WC1V 9NS','0207 321 8002',5000.00,0.00,'VARIABLE',0.0000,'Normal',NULL,NULL,0.00,NULL);
/*!40000 ALTER TABLE `merchants` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_catalogue`
--

DROP TABLE IF EXISTS `supplier_catalogue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier_catalogue` (
  `item_id` varchar(20) NOT NULL,
  `description` varchar(255) NOT NULL,
  `package_type` varchar(50) DEFAULT NULL,
  `unit` varchar(20) DEFAULT NULL,
  `units_per_pack` int DEFAULT NULL,
  `package_cost` decimal(10,2) NOT NULL,
  `availability` int DEFAULT '0',
  `stock_limit` int DEFAULT '0',
  `category` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_catalogue`
--

LOCK TABLES `supplier_catalogue` WRITE;
/*!40000 ALTER TABLE `supplier_catalogue` DISABLE KEYS */;
INSERT INTO `supplier_catalogue` VALUES ('100 00001','Paracetamol','box','Caps',20,0.10,10345,300,'Pain Relief'),('100 00002','Aspirin','box','Caps',20,0.50,12453,500,'Pain Relief'),('100 00003','Analgin','box','Caps',10,1.20,4235,200,'Pain Relief'),('100 00004','Celebrex, caps 100 mg','box','Caps',10,10.00,3420,200,'Pain Relief'),('100 00005','Celebrex, caps 200 mg','box','caps',10,18.50,1450,150,'Pain Relief'),('100 00006','Retin-A Tretin, 30 g','box','caps',20,25.00,2013,200,'Pain Relief'),('100 00007','Lipitor TB, 20 mg','box','caps',30,15.50,1562,200,'Pain Relief'),('100 00008','Claritin CR, 60g','box','caps',20,19.50,2540,200,'Pain Relief'),('200 00004','Iodine tincture','bottle','ml',100,0.30,22134,200,'Antiseptics'),('200 00005','Rhynol','bottle','ml',200,2.50,1908,300,'Cold Relief'),('300 00001','Ospen','box','caps',20,10.50,809,200,'Antibiotics'),('300 00002','Amopen','box','caps',30,15.00,1340,300,'Antibiotics'),('400 00001','Vitamin C','box','caps',30,1.20,3258,300,'Vitamins'),('400 00002','Vitamin B12','box','caps',30,1.30,2673,300,'Vitamins');
/*!40000 ALTER TABLE `supplier_catalogue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_invoices`
--

DROP TABLE IF EXISTS `supplier_invoices`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier_invoices` (
  `invoice_id` varchar(20) NOT NULL,
  `order_id` varchar(20) DEFAULT NULL,
  `merchant_account_id` int DEFAULT NULL,
  `invoice_date` date DEFAULT NULL,
  `amount` decimal(10,2) NOT NULL,
  `paid_amount` decimal(10,2) DEFAULT '0.00',
  `outstanding_balance` decimal(10,2) NOT NULL,
  `due_date` date DEFAULT NULL,
  `status` varchar(20) DEFAULT 'Unpaid',
  PRIMARY KEY (`invoice_id`),
  KEY `order_id` (`order_id`),
  KEY `merchant_account_id` (`merchant_account_id`),
  CONSTRAINT `supplier_invoices_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `supplier_orders` (`order_id`) ON DELETE SET NULL,
  CONSTRAINT `supplier_invoices_ibfk_2` FOREIGN KEY (`merchant_account_id`) REFERENCES `merchants` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_invoices`
--

LOCK TABLES `supplier_invoices` WRITE;
/*!40000 ALTER TABLE `supplier_invoices` DISABLE KEYS */;
INSERT INTO `supplier_invoices` VALUES ('INV-0001','IP0001',NULL,'2026-04-12',0.10,0.00,0.10,'2026-05-12','Unpaid');
/*!40000 ALTER TABLE `supplier_invoices` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_order_items`
--

DROP TABLE IF EXISTS `supplier_order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  KEY `item_id` (`item_id`),
  CONSTRAINT `supplier_order_items_ibfk_1` FOREIGN KEY (`order_id`) REFERENCES `supplier_orders` (`order_id`) ON DELETE CASCADE,
  CONSTRAINT `supplier_order_items_ibfk_2` FOREIGN KEY (`item_id`) REFERENCES `supplier_catalogue` (`item_id`) ON DELETE SET NULL
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_order_items`
--

LOCK TABLES `supplier_order_items` WRITE;
/*!40000 ALTER TABLE `supplier_order_items` DISABLE KEYS */;
INSERT INTO `supplier_order_items` VALUES (1,'IP0001','100 00001','Paracetamol',1,0.10,0.10);
/*!40000 ALTER TABLE `supplier_order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `supplier_orders`
--

DROP TABLE IF EXISTS `supplier_orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `supplier_orders` (
  `order_id` varchar(20) NOT NULL,
  `merchant_account_id` int DEFAULT NULL,
  `order_date` date NOT NULL,
  `total_amount` decimal(10,2) NOT NULL,
  `status` varchar(20) DEFAULT 'Ordered',
  `dispatched_date` date DEFAULT NULL,
  `delivered_date` date DEFAULT NULL,
  `paid_date` date DEFAULT NULL,
  `payment_status` varchar(20) DEFAULT 'Pending',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`order_id`),
  KEY `merchant_account_id` (`merchant_account_id`),
  CONSTRAINT `supplier_orders_ibfk_1` FOREIGN KEY (`merchant_account_id`) REFERENCES `merchants` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `supplier_orders`
--

LOCK TABLES `supplier_orders` WRITE;
/*!40000 ALTER TABLE `supplier_orders` DISABLE KEYS */;
INSERT INTO `supplier_orders` VALUES ('IP0001',NULL,'2026-04-12',0.10,'Delivered',NULL,'2026-04-12',NULL,'Pending','2026-04-12 22:28:00');
/*!40000 ALTER TABLE `supplier_orders` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-15 17:15:17
CREATE DATABASE  IF NOT EXISTS `ipos_ca` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `ipos_ca`;
-- MySQL dump 10.13  Distrib 8.0.44, for macos15 (arm64)
--
-- Host: 127.0.0.1    Database: ipos_ca
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'c812522e-261d-11f1-a08b-697e3eb8e349:1-608';

--
-- Table structure for table `customer_payments`
--

DROP TABLE IF EXISTS `customer_payments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_payments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `customer_id` int NOT NULL,
  `payment_date` date NOT NULL,
  `amount` decimal(10,2) NOT NULL,
  `payment_type` varchar(20) NOT NULL,
  `payment_details` varchar(100) DEFAULT NULL,
  `reference` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `customer_id` (`customer_id`),
  CONSTRAINT `customer_payments_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customer_payments`
--

LOCK TABLES `customer_payments` WRITE;
/*!40000 ALTER TABLE `customer_payments` DISABLE KEYS */;
INSERT INTO `customer_payments` VALUES (3,102,'2026-03-29',150.00,'CARD','Credit Card','Payment for balance'),(4,101,'2026-02-28',50.00,'CARD','Debit Card','Last payment');
/*!40000 ALTER TABLE `customer_payments` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Temporary view structure for view `customer_purchase_history`
--

DROP TABLE IF EXISTS `customer_purchase_history`;
/*!50001 DROP VIEW IF EXISTS `customer_purchase_history`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `customer_purchase_history` AS SELECT 
 1 AS `customer_id`,
 1 AS `customer_name`,
 1 AS `sale_id`,
 1 AS `sale_date`,
 1 AS `total_with_vat`,
 1 AS `payment_type`,
 1 AS `payment_details`,
 1 AS `is_account_holder`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `customers`
--

DROP TABLE IF EXISTS `customers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `last_payment_date` date DEFAULT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `customers`
--

LOCK TABLES `customers` WRITE;
/*!40000 ALTER TABLE `customers` DISABLE KEYS */;
INSERT INTO `customers` VALUES (101,'Ms','Eva Bauyer','eva@example.com','1, Liverpool street','London','EC2V 8NS',500.00,0.00,NULL,'Normal','FIXED',0.0300,0.00,'no_need','no_need',NULL,NULL,NULL),(102,'Mr','Glynne Morrison','glynne@example.com','1, Liverpool street','London','EC2V 8NS',500.00,0.00,NULL,'Normal','VARIABLE',0.0000,0.00,'no_need','no_need',NULL,NULL,NULL);
/*!40000 ALTER TABLE `customers` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `document_templates`
--

DROP TABLE IF EXISTS `document_templates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `document_templates`
--

LOCK TABLES `document_templates` WRITE;
/*!40000 ALTER TABLE `document_templates` DISABLE KEYS */;
INSERT INTO `document_templates` VALUES (1,'Default Invoice','INVOICE','Invoice No. {invoice_number}','Dear {customer_title} {customer_last_name},\n\nThank you for your valued custom.\n\nInvoice Number: {invoice_number}\nTotal Amount Due: {total_amount}','Thank you for your valued custom.\n\nYours sincerely,\n\n{director_name}\nDirector of Operations\n{company_name}',1,'2026-04-12 21:53:36'),(2,'Default Monthly Statement','MONTHLY_STATEMENT','Monthly Statement - {month} {year}','Dear {customer_title} {customer_last_name},\n\nPlease find your monthly statement attached.\n\nTotal Outstanding: {total_outstanding}','Thank you for your continued custom.\n\nYours sincerely,\n\n{director_name}\n{company_name}',1,'2026-04-12 21:53:36'),(3,'Default First Reminder','FIRST_REMINDER','First Payment Reminder - Invoice {invoice_number}','Dear {customer_title} {customer_last_name},\n\nAccording to our records, payment of {total_amount} is now due.\n\nPlease settle this invoice at your earliest convenience.','We value your business.\n\nYours sincerely,\n\n{director_name}\n{company_name}',1,'2026-04-12 21:53:36'),(4,'Default Second Reminder','SECOND_REMINDER','Second Payment Reminder - Invoice {invoice_number}','Dear {customer_title} {customer_last_name},\n\nThis is our second reminder regarding the outstanding payment of {total_amount}.\n\nPlease arrange payment immediately to avoid further action.','We value your business.\n\nYours sincerely,\n\n{director_name}\n{company_name}',1,'2026-04-12 21:53:36');
/*!40000 ALTER TABLE `document_templates` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `merchant_settings`
--

DROP TABLE IF EXISTS `merchant_settings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `vat_rate` decimal(5,4) DEFAULT '0.0000',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `merchant_settings`
--

LOCK TABLES `merchant_settings` WRITE;
/*!40000 ALTER TABLE `merchant_settings` DISABLE KEYS */;
INSERT INTO `merchant_settings` VALUES (1,'Cosymed Ltd','3, High Level Drive',NULL,'Sydenham','SE26 3ET','0208 778 0124','0208 778 0125','accounts@cosymed.local',NULL,NULL,NULL,NULL,0.0000,'2026-04-12 21:53:36');
/*!40000 ALTER TABLE `merchant_settings` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `products`
--

DROP TABLE IF EXISTS `products`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `products` (
  `id` int NOT NULL,
  `supplier_item_id` varchar(50) DEFAULT NULL,
  `name` varchar(255) NOT NULL,
  `bulk_cost` decimal(10,2) NOT NULL,
  `markup_rate` decimal(5,2) NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `stock` int NOT NULL,
  `low_stock_threshold` int NOT NULL,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_supplier_item_id` (`supplier_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `products`
--

LOCK TABLES `products` WRITE;
/*!40000 ALTER TABLE `products` DISABLE KEYS */;
INSERT INTO `products` VALUES (10000001,'10000001','Paracetamol',0.10,1.00,0.20,125,10,'2026-04-14 01:44:10'),(10000002,'10000002','Aspirin',0.50,1.00,1.00,201,15,'2026-04-14 01:44:10'),(10000003,'10000003','Analgin',1.20,1.00,2.40,25,10,'2026-04-14 01:44:10'),(10000004,'10000004','Celebrex, caps 100 mg',10.00,1.00,20.00,43,10,'2026-04-14 01:44:10'),(10000005,'10000005','Celebrex, caps 200 mg',18.50,1.00,37.00,35,5,'2026-04-14 01:44:10'),(10000006,'100 00006','Retin-A Tretin, 30 g',25.00,1.00,50.00,28,10,'2026-04-14 01:44:10'),(10000007,'100 00007','Lipitor TB, 20 mg',15.50,1.00,31.00,10,10,'2026-04-14 01:44:10'),(10000008,'100 00008','Claritin CR, 60g',19.50,1.00,39.00,21,10,'2026-04-14 01:44:10'),(20000004,'20000004','Iodine tincture',0.30,1.00,0.60,35,10,'2026-04-14 01:44:10'),(20000005,'20000005','Rhynol',2.50,1.00,5.00,14,15,'2026-04-14 01:44:10'),(30000001,'30000001','Ospen',10.50,1.00,21.00,78,10,'2026-04-14 01:44:10'),(30000002,'30000002','Amopen',15.00,1.00,30.00,87,15,'2026-04-15 12:18:53'),(40000001,'40000001','Vitamin C',1.20,1.00,2.40,22,15,'2026-04-14 01:44:10'),(40000002,'40000002','Vitamin B12',1.30,1.00,2.60,43,15,'2026-04-14 01:44:10');
/*!40000 ALTER TABLE `products` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sales`
--

DROP TABLE IF EXISTS `sales`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
  `payment_details` varchar(100) DEFAULT NULL,
  `sale_date` date NOT NULL,
  PRIMARY KEY (`id`),
  KEY `customer_id` (`customer_id`),
  CONSTRAINT `sales_ibfk_1` FOREIGN KEY (`customer_id`) REFERENCES `customers` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sales`
--

LOCK TABLES `sales` WRITE;
/*!40000 ALTER TABLE `sales` DISABLE KEYS */;
INSERT INTO `sales` VALUES (50001,101,'Eva Bauyer',1,168.00,0.0300,5.04,162.96,0.00,162.96,'CARD',NULL,'2026-03-01'),(50002,NULL,'Walk-in Customer',0,12.00,0.0000,0.00,12.00,0.00,12.00,'CASH',NULL,'2026-03-03'),(50003,NULL,'Walk-in Customer',0,114.00,0.0000,0.00,114.00,0.00,114.00,'CARD',NULL,'2026-03-03'),(50004,NULL,'Walk-in Customer',0,54.00,0.0000,0.00,54.00,0.00,54.00,'CASH',NULL,'2026-03-03'),(50005,NULL,'Walk-in Customer',0,58.00,0.0000,0.00,58.00,0.00,58.00,'CASH',NULL,'2026-03-03'),(50006,NULL,'Walk-in Customer',0,52.00,0.0000,0.00,52.00,0.00,52.00,'CARD',NULL,'2026-03-03'),(50007,NULL,'Walk-in Customer',0,96.00,0.0000,0.00,96.00,0.00,96.00,'CASH',NULL,'2026-03-03'),(50008,102,'Glynne Morrison',1,228.00,0.0000,0.00,228.00,0.00,228.00,'CARD',NULL,'2026-03-05'),(50009,101,'Eva Bauyer',1,187.00,0.0300,5.61,181.39,0.00,181.39,'CARD',NULL,'2026-04-01');
/*!40000 ALTER TABLE `sales` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stock_changes`
--

DROP TABLE IF EXISTS `stock_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stock_changes`
--

LOCK TABLES `stock_changes` WRITE;
/*!40000 ALTER TABLE `stock_changes` DISABLE KEYS */;
/*!40000 ALTER TABLE `stock_changes` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `full_name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(20) NOT NULL,
  `active` tinyint(1) DEFAULT '1',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES (1,'sysdba','System Administrator','masterkey','ADMIN',1),(2,'manager','Director of Operations','Get_it_done','MANAGER',1),(3,'accountant','Senior Accountant','Count_money','ACCOUNTANT',1),(4,'clerk','Account Clerk','Paperwork','CLERK',1),(6,'YT','YT','YT','PHARMACIST',1);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Final view structure for view `customer_purchase_history`
--

/*!50001 DROP VIEW IF EXISTS `customer_purchase_history`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`localhost` SQL SECURITY DEFINER */
/*!50001 VIEW `customer_purchase_history` AS select `c`.`id` AS `customer_id`,`c`.`name` AS `customer_name`,`s`.`id` AS `sale_id`,`s`.`sale_date` AS `sale_date`,`s`.`total_with_vat` AS `total_with_vat`,`s`.`payment_type` AS `payment_type`,`s`.`payment_details` AS `payment_details`,`s`.`is_account_holder` AS `is_account_holder` from (`customers` `c` left join `sales` `s` on((`c`.`id` = `s`.`customer_id`))) order by `c`.`id`,`s`.`sale_date` desc */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-15 17:15:17
CREATE DATABASE  IF NOT EXISTS `catalogue` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `catalogue`;
-- MySQL dump 10.13  Distrib 8.0.44, for macos15 (arm64)
--
-- Host: 127.0.0.1    Database: catalogue
-- ------------------------------------------------------
-- Server version	9.6.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
SET @MYSQLDUMP_TEMP_LOG_BIN = @@SESSION.SQL_LOG_BIN;
SET @@SESSION.SQL_LOG_BIN= 0;

--
-- GTID state at the beginning of the backup 
--

SET @@GLOBAL.GTID_PURGED=/*!80000 '+'*/ 'c812522e-261d-11f1-a08b-697e3eb8e349:1-608';

--
-- Table structure for table `catalogue`
--

DROP TABLE IF EXISTS `catalogue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `catalogue` (
  `ItemID` int NOT NULL,
  `Descriptions` varchar(255) NOT NULL,
  `PackageType` varchar(50) NOT NULL,
  `Unit` varchar(50) NOT NULL,
  `UnitsInAPack` int NOT NULL,
  `PackageCost` decimal(10,2) NOT NULL,
  `Availability` int NOT NULL,
  `StockLimit` int NOT NULL,
  `is_active` tinyint(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`ItemID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `catalogue`
--

LOCK TABLES `catalogue` WRITE;
/*!40000 ALTER TABLE `catalogue` DISABLE KEYS */;
INSERT INTO `catalogue` VALUES (10000001,'Paracetamol','box','Caps',20,0.10,10345,300,1),(10000002,'Aspirin','box','Caps',20,0.50,12453,500,1),(10000003,'Analgin','box','Caps',10,1.20,4235,200,1),(10000004,'Celebrex, caps 100 mg','box','caps',10,10.00,3420,200,1),(10000005,'Celebrex, caps 200 mg','box','caps',10,18.50,1450,150,1),(10000006,'Retin-A Tretin, 30 g','box','caps',20,25.00,2013,200,1),(10000007,'Lipitor TB, 20 mg','box','caps',30,15.50,1562,200,1),(10000008,'Claritin CR, 60g','box','caps',20,19.50,2540,200,1),(20000004,'Iodine tincture','bottle','ml',100,0.30,2134,200,1),(20000005,'Rhynol','bottle','ml',200,2.50,1908,300,1),(30000001,'Ospen','box','caps',20,10.50,809,200,1),(30000002,'Amopen','box','caps',30,15.00,1340,300,1),(40000001,'Vitamin C','box','caps',30,1.20,3258,300,1),(40000002,'Vitamin B12','box','caps',30,1.30,2673,300,1);
/*!40000 ALTER TABLE `catalogue` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `commercial_applications`
--

DROP TABLE IF EXISTS `commercial_applications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commercial_applications` (
  `applicationId` bigint NOT NULL AUTO_INCREMENT,
  `accountNo` varchar(20) DEFAULT NULL,
  `companyName` varchar(120) NOT NULL,
  `businessAddress` varchar(255) NOT NULL,
  `companyRegistration` varchar(30) NOT NULL,
  `companyDirector` varchar(120) DEFAULT NULL,
  `typeOfBusiness` varchar(80) DEFAULT NULL,
  `emailAddress` varchar(255) NOT NULL,
  `submittedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) NOT NULL DEFAULT 'SUBMITTED',
  PRIMARY KEY (`applicationId`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `commercial_applications`
--

LOCK TABLES `commercial_applications` WRITE;
/*!40000 ALTER TABLE `commercial_applications` DISABLE KEYS */;
INSERT INTO `commercial_applications` VALUES (1,'PU0003','Pond Pharmacy','25, High Street, Chislehurst, BR7 5BN','UK10003429','CompH','Pharmacy','pondPharma@example.com','2026-04-15 17:14:39','SUBMITTED');
/*!40000 ALTER TABLE `commercial_applications` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `member`
--

DROP TABLE IF EXISTS `member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `member` (
  `emailAddress` varchar(255) NOT NULL,
  `password` varchar(20) NOT NULL,
  `type` varchar(15) NOT NULL,
  `totalPurchases` int DEFAULT NULL,
  `firstLogin` tinyint DEFAULT NULL,
  PRIMARY KEY (`emailAddress`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `member`
--

LOCK TABLES `member` WRITE;
/*!40000 ALTER TABLE `member` DISABLE KEYS */;
INSERT INTO `member` VALUES ('cool@example.com','12ss_56_SS','NonCommercial',8,0),('cool1@example.com','34pp_78_LL','NonCommercial',0,0),('estroyer221@gmail.com','p','NonCommercial',9,0),('manager','GetPU_it_done','PU-Admin',0,0),('sysdba','masterkey','Administrator',0,0);
/*!40000 ALTER TABLE `member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
  `OrderItemID` bigint NOT NULL AUTO_INCREMENT,
  `OrderID` char(36) NOT NULL,
  `ItemID` int NOT NULL,
  `Quantity` int NOT NULL,
  `UnitPrice` decimal(10,2) NOT NULL,
  `ItemDescription` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`OrderItemID`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES (1,'33f229da-38e6-11f1-a0c5-5a4e776cb64c',10000001,2,0.10,'Paracetamol'),(2,'33f229da-38e6-11f1-a0c5-5a4e776cb64c',10000002,1,0.50,'Aspirin'),(3,'33f3cb82-38e6-11f1-a0c5-5a4e776cb64c',40000001,3,1.20,'Vitamin C'),(4,'33f43b9e-38e6-11f1-a0c5-5a4e776cb64c',30000001,1,10.50,'Ospen');
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
  `OrderID` char(36) NOT NULL,
  `Descriptions` varchar(255) DEFAULT NULL,
  `Address` varchar(255) DEFAULT NULL,
  `DeliveryType` varchar(255) DEFAULT NULL,
  `OrderStatus` varchar(255) DEFAULT NULL,
  `EmailAddress` varchar(255) DEFAULT NULL,
  `CreatedAt` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`OrderID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES ('33f229da-38e6-11f1-a0c5-5a4e776cb64c','Order: Paracetamol x2, Aspirin x1','1 Avery',NULL,'Pending','estroyer221@gmail.com','2026-04-15 17:14:39'),('33f3cb82-38e6-11f1-a0c5-5a4e776cb64c','Order: Vitamin C x3','2 Avery',NULL,'Pending','estroyer221@gmail.com','2026-04-15 17:14:39'),('33f43b9e-38e6-11f1-a0c5-5a4e776cb64c','Order: Ospen x1',NULL,NULL,'Pending','guest.user@ipos.com','2026-04-15 17:14:39');
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `promotion_campaign_items`
--

DROP TABLE IF EXISTS `promotion_campaign_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_campaign_items` (
  `campaign_item_id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `product_id` varchar(20) NOT NULL,
  `discount_percent` decimal(5,2) NOT NULL DEFAULT '0.00',
  `promotional_price` decimal(10,2) NOT NULL DEFAULT '0.00',
  `added_to_order_count` int NOT NULL DEFAULT '0',
  `purchased_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`campaign_item_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `promotion_campaign_items`
--

LOCK TABLES `promotion_campaign_items` WRITE;
/*!40000 ALTER TABLE `promotion_campaign_items` DISABLE KEYS */;
INSERT INTO `promotion_campaign_items` VALUES (1,1,'10000002',10.00,0.45,5,2),(2,1,'10000003',15.00,0.09,3,1),(3,2,'10000005',20.00,3.20,2,1);
/*!40000 ALTER TABLE `promotion_campaign_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `promotion_campaigns`
--

DROP TABLE IF EXISTS `promotion_campaigns`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_campaigns` (
  `campaign_id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_code` varchar(20) NOT NULL,
  `title` varchar(150) NOT NULL,
  `descriptions` text,
  `start_datetime` datetime NOT NULL,
  `end_datetime` datetime NOT NULL,
  `discount_percent` double NOT NULL,
  `status` enum('SCHEDULED','ACTIVE','EXPIRED','CANCELLED') NOT NULL DEFAULT 'SCHEDULED',
  `cancelled_at` datetime DEFAULT NULL,
  `click_count` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`campaign_id`),
  UNIQUE KEY `campaign_code` (`campaign_code`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `promotion_campaigns`
--

LOCK TABLES `promotion_campaigns` WRITE;
/*!40000 ALTER TABLE `promotion_campaigns` DISABLE KEYS */;
INSERT INTO `promotion_campaigns` VALUES (1,'CAMP_001','Spring Sale','Promo campaign','2026-04-01 00:00:00','2026-08-01 00:00:00',1,'ACTIVE',NULL,10),(2,'CAMP_002','Weekend Offer','Short promo','2026-04-10 00:00:00','2026-07-01 00:00:00',0.5,'ACTIVE',NULL,5);
/*!40000 ALTER TABLE `promotion_campaigns` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `promotion_order_events`
--

DROP TABLE IF EXISTS `promotion_order_events`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `promotion_order_events` (
  `event_id` bigint NOT NULL AUTO_INCREMENT,
  `campaign_id` bigint NOT NULL,
  `campaign_item_id` bigint NOT NULL,
  `product_id` varchar(20) NOT NULL,
  `event_type` enum('ADDED','PURCHASED') NOT NULL,
  `quantity` int NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `order_reference` varchar(50) DEFAULT NULL,
  `event_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`event_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `promotion_order_events`
--

LOCK TABLES `promotion_order_events` WRITE;
/*!40000 ALTER TABLE `promotion_order_events` DISABLE KEYS */;
INSERT INTO `promotion_order_events` VALUES (1,1,1,'10000002','ADDED',5,0.45,NULL,'2026-04-15 17:14:39'),(2,1,1,'10000002','PURCHASED',2,0.45,NULL,'2026-04-15 17:14:39'),(3,1,2,'10000003','ADDED',3,0.09,NULL,'2026-04-15 17:14:39'),(4,2,3,'10000005','PURCHASED',1,3.20,NULL,'2026-04-15 17:14:39');
/*!40000 ALTER TABLE `promotion_order_events` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `transaction`
--

DROP TABLE IF EXISTS `transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `transaction` (
  `TransactionID` int NOT NULL AUTO_INCREMENT,
  `Amount` double DEFAULT NULL,
  `BillingAddress` varchar(255) DEFAULT NULL,
  `CardNumber` varchar(20) DEFAULT NULL,
  `CVV` int DEFAULT NULL,
  `PurchaseDate` varchar(255) DEFAULT NULL,
  `EmailAddress` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`TransactionID`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `transaction`
--

LOCK TABLES `transaction` WRITE;
/*!40000 ALTER TABLE `transaction` DISABLE KEYS */;
INSERT INTO `transaction` VALUES (1,120,NULL,NULL,NULL,NULL,'demo.member@ipos.com'),(2,55,NULL,NULL,NULL,NULL,'estroyer221@gmail.com'),(3,60,NULL,NULL,NULL,NULL,'estroyer221@gmail.com'),(4,55,NULL,NULL,NULL,NULL,'demo.member@ipos.com'),(5,999,NULL,NULL,NULL,NULL,'guest.user@ipos.com');
/*!40000 ALTER TABLE `transaction` ENABLE KEYS */;
UNLOCK TABLES;
SET @@SESSION.SQL_LOG_BIN = @MYSQLDUMP_TEMP_LOG_BIN;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-04-15 17:15:17
