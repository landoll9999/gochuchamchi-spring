-- gochuchamchi 최종 스키마
-- 변경 이력:
--   - users: name/phone/birthdate/gender/nationality/address 컬럼 추가 (애플리케이션 실제 사용 컬럼 기준)
--   - 전체 테이블 utf8mb4로 통일 (한글 등 멀티바이트 문자 저장 문제 해결)

CREATE DATABASE IF NOT EXISTS gochuchamchi
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE gochuchamchi;

CREATE TABLE IF NOT EXISTS users (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  username varchar(50) NOT NULL,
  name varchar(50) NOT NULL DEFAULT '',
  password varchar(255) NOT NULL,
  phone varchar(20) DEFAULT NULL,
  birthdate varchar(8) DEFAULT NULL,
  gender varchar(1) DEFAULT NULL,
  nationality varchar(20) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  email varchar(100) DEFAULT NULL,
  role varchar(20) DEFAULT 'user',
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS notices (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  title varchar(255) NOT NULL,
  content text NOT NULL,
  author varchar(50) NOT NULL,
  pinned tinyint(1) NOT NULL DEFAULT 0,
  views int(11) NOT NULL DEFAULT 0,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  seller_id bigint(20) NOT NULL,
  brand varchar(100) NOT NULL,
  name varchar(255) NOT NULL,
  category varchar(50) NOT NULL,
  price int(11) NOT NULL,
  stock int(11) NOT NULL DEFAULT 0,
  image varchar(500) DEFAULT NULL,
  description text DEFAULT NULL,
  new_item tinyint(1) NOT NULL DEFAULT 1,
  active tinyint(1) NOT NULL DEFAULT 1,
  view_count int(11) NOT NULL DEFAULT 0,
  created_at datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_sizes (
  id int(10) unsigned NOT NULL AUTO_INCREMENT,
  product_id int(10) unsigned NOT NULL,
  size_name varchar(10) NOT NULL,
  stock int(10) unsigned NOT NULL DEFAULT 0,
  sort_order int(10) unsigned NOT NULL DEFAULT 0,
  sold_out tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
