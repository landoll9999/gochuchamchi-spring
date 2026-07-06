CREATE DATABASE IF NOT EXISTS gochuchamchi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE gochuchamchi;

CREATE TABLE IF NOT EXISTS users (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(20)  NOT NULL UNIQUE,
  name        VARCHAR(50)  NOT NULL,
  email       VARCHAR(100) DEFAULT NULL,
  password    VARCHAR(255) NOT NULL,
  phone       VARCHAR(20)  DEFAULT NULL,
  birthdate   CHAR(8)      DEFAULT NULL,
  gender      CHAR(1)      DEFAULT 'M',
  nationality VARCHAR(10)  DEFAULT 'domestic',
  address     VARCHAR(300) DEFAULT NULL,
  role        ENUM('user','seller','admin') NOT NULL DEFAULT 'user',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_username (username),
  INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS products (
  id          INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  seller_id   INT UNSIGNED NOT NULL,
  brand       VARCHAR(100) NOT NULL,
  name        VARCHAR(200) NOT NULL,
  category    VARCHAR(50)  NOT NULL,
  price       INT UNSIGNED NOT NULL,
  stock       INT UNSIGNED NOT NULL DEFAULT 0,
  image       VARCHAR(500) DEFAULT NULL,
  description TEXT         DEFAULT NULL,
  is_new      TINYINT(1)   NOT NULL DEFAULT 1,
  is_active   TINYINT(1)   NOT NULL DEFAULT 1,
  view_count  INT UNSIGNED NOT NULL DEFAULT 0,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (seller_id) REFERENCES users(id),
  INDEX idx_category (category),
  INDEX idx_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS notices (
  id         INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  title      VARCHAR(300) NOT NULL,
  content    TEXT NOT NULL,
  is_pinned  TINYINT(1) NOT NULL DEFAULT 0,
  views      INT UNSIGNED NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO notices (title, content, is_pinned) VALUES
('gochuchamchi 오픈 안내', '안녕하세요, gochuchamchi입니다.\n\n2025년 서울에서 시작한 스트리트웨어 브랜드입니다.', 1),
('배송 안내', '주문 후 1~3 영업일 내 발송됩니다.', 0),
('교환 및 환불 안내', '상품 수령 후 7일 이내 교환/환불이 가능합니다.', 0);
