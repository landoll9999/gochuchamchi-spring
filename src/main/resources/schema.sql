CREATE TABLE IF NOT EXISTS users (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  username varchar(50) NOT NULL,
  name varchar(50) DEFAULT NULL,
  password varchar(255) NOT NULL,
  email varchar(100) DEFAULT NULL,
  phone varchar(20) DEFAULT NULL,
  birthdate varchar(20) DEFAULT NULL,
  gender varchar(10) DEFAULT NULL,
  nationality varchar(50) DEFAULT NULL,
  address varchar(255) DEFAULT NULL,
  role varchar(20) DEFAULT 'user',
  created_at datetime DEFAULT NOW(),
  PRIMARY KEY (id),
  UNIQUE KEY username (username)
);

CREATE TABLE IF NOT EXISTS notices (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  title varchar(255) NOT NULL,
  content text NOT NULL,
  author varchar(50) NOT NULL,
  pinned tinyint(1) NOT NULL DEFAULT 0,
  views int(11) NOT NULL DEFAULT 0,
  created_at datetime DEFAULT NOW(),
  PRIMARY KEY (id)
);

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
  created_at datetime DEFAULT NOW(),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS product_sizes (
  id int(10) unsigned NOT NULL AUTO_INCREMENT,
  product_id int(10) unsigned NOT NULL,
  size_name varchar(10) NOT NULL,
  stock int(10) unsigned NOT NULL DEFAULT 0,
  sort_order int(10) unsigned NOT NULL DEFAULT 0,
  sold_out tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY product_id (product_id)
);