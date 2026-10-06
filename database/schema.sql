-- ============================================================================
-- BloodConnect — Blood Donation Management System
-- Database Schema Script (MySQL 8.0)
-- Small Academic Project Edition (6 Tables Only)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS bloodconnect_db 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE bloodconnect_db;

-- ----------------------------------------------------------------------------
-- 1. Table: users
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL, -- 'CUSTOMER', 'HOSPITAL', 'ADMIN'
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 2. Table: donors
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    blood_group VARCHAR(5) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(15) NOT NULL,
    city VARCHAR(100) NOT NULL,
    pincode VARCHAR(15) NOT NULL,
    last_donation_date DATE NULL,
    is_eligible BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 3. Table: hospitals
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS hospitals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    hospital_name VARCHAR(150) NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    pincode VARCHAR(15) NOT NULL,
    contact_person VARCHAR(100) NOT NULL,
    contact_phone VARCHAR(20) NOT NULL,
    verification_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED'
    verified_at DATETIME NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 4. Table: blood_inventory
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    units_available INT NOT NULL DEFAULT 0,
    last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_hospital_blood_group (hospital_id, blood_group),
    CONSTRAINT chk_inventory_non_neg CHECK (units_available >= 0),
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 5. Table: donations
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS donations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    hospital_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    scheduled_date DATE NOT NULL,
    time_slot VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED', -- 'SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED'
    remarks TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- 6. Table: blood_requests
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS blood_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hospital_id BIGINT NOT NULL,
    blood_group VARCHAR(5) NOT NULL,
    required_units INT NOT NULL,
    fulfilled_units INT NOT NULL DEFAULT 0,
    urgency_level VARCHAR(20) NOT NULL, -- 'ROUTINE', 'URGENT', 'CRITICAL'
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'FULFILLED', 'REJECTED', 'CANCELLED'
    notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ----------------------------------------------------------------------------
-- Seed: Default Administrator Account
-- Email: admin@bloodconnect.com
-- Password: Admin@123
-- BCrypt: $2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.
-- ----------------------------------------------------------------------------
INSERT INTO users (email, password_hash, full_name, phone, role, is_active, created_at)
SELECT 'admin@bloodconnect.com', 
       '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 
       'System Administrator', 
       '9876543210', 
       'ADMIN', 
       TRUE, 
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@bloodconnect.com');
