--------------------------------------------------
-- テーブル削除
--------------------------------------------------
DROP TABLE IF EXISTS reports cascade;
DROP TABLE IF EXISTS search_keywords cascade;
DROP TABLE IF EXISTS ng_keywords cascade;
DROP TABLE IF EXISTS login_history cascade;
DROP TABLE IF EXISTS item_images cascade;
DROP TABLE IF EXISTS categories cascade;
DROP TABLE IF EXISTS messages cascade;
DROP TABLE IF EXISTS transactions cascade;
DROP TABLE IF EXISTS applications cascade;
DROP TABLE IF EXISTS items cascade;
DROP TABLE IF EXISTS users cascade;

--------------------------------------------------
-- users（社員）
--------------------------------------------------
CREATE TABLE users
(
  user_id INTEGER PRIMARY KEY,
  name VARCHAR NOT NULL,
  email VARCHAR NOT NULL UNIQUE,
  password VARCHAR(64) NOT NULL,
  role INTEGER NOT NULL,
  department VARCHAR,
  status INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL,
  icon_url VARCHAR,

  login_at TIMESTAMP NOT NULL,
  temporary_password BOOLEAN NOT NULL,
  last_password_change TIMESTAMP NOT NULL,
  login_fail_count INTEGER NOT NULL DEFAULT 0

);

--------------------------------------------------
-- categories（カテゴリ）
--------------------------------------------------
CREATE TABLE categories
(
  category_id SERIAL PRIMARY KEY,
  name VARCHAR NOT NULL
);

--------------------------------------------------
-- items（物品）
--------------------------------------------------
CREATE TABLE items
(
  item_id SERIAL PRIMARY KEY,
  user_id INTEGER NOT NULL,
  name VARCHAR NOT NULL,
  description VARCHAR,
  condition INTEGER NOT NULL,
  price NUMERIC(8,2),
  type INTEGER NOT NULL,
  status INTEGER NOT NULL,
  category_id INTEGER NOT NULL,
  deadline DATE NOT NULL,
  created_at TIMESTAMP NOT NULL,
  place INTEGER NOT NULL
);

--------------------------------------------------
-- item_images（画像）
--------------------------------------------------
CREATE TABLE item_images
(
  image_id SERIAL PRIMARY KEY,
  item_id INTEGER NOT NULL,
  image_url VARCHAR NOT NULL
);

--------------------------------------------------
-- applications（応募）
--------------------------------------------------
CREATE TABLE applications
(
  application_id SERIAL PRIMARY KEY,
  item_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  bid_price NUMERIC(8,2),
  status INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL
);

--------------------------------------------------
-- transactions（取引）
--------------------------------------------------
CREATE TABLE transactions
(
  transaction_id SERIAL PRIMARY KEY,
  item_id INTEGER NOT NULL,
  seller_id INTEGER NOT NULL,
  buyer_id INTEGER NOT NULL,
  status INTEGER NOT NULL,
  seller_completed INTEGER NOT NULL DEFAULT 0,
  buyer_completed INTEGER NOT NULL DEFAULT 0,
  transfer_code VARCHAR(50) UNIQUE,
  completed_at TIMESTAMP
);

--------------------------------------------------
-- messages（メッセージ）
--------------------------------------------------
CREATE TABLE messages
(
  message_id SERIAL PRIMARY KEY,
  sender_id INTEGER NOT NULL,
  receiver_id INTEGER NOT NULL,
  item_id INTEGER,
  content VARCHAR NOT NULL,
  created_at TIMESTAMP NOT NULL
);



--------------------------------------------------
-- ng_keywords（NGワード）
--------------------------------------------------
CREATE TABLE ng_keywords
(
  keyword_id SERIAL PRIMARY KEY,
  keyword VARCHAR NOT NULL,
  type INTEGER NOT NULL,
  status INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL
);

--------------------------------------------------
-- search_keywords（検索キー）
--------------------------------------------------
CREATE TABLE search_keywords
(
  keyword_id SERIAL PRIMARY KEY,
  keyword VARCHAR NOT NULL UNIQUE,
  status INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL
);

--------------------------------------------------
-- reports（通報）
--------------------------------------------------
CREATE TABLE reports
(
  report_id SERIAL PRIMARY KEY,
  target_type INTEGER NOT NULL,
  target_id INTEGER NOT NULL,
  user_id INTEGER NOT NULL,
  reason VARCHAR NOT NULL,
  status INTEGER NOT NULL,
  created_at TIMESTAMP NOT NULL
);

--------------------------------------------------
-- 外部キー制約
--------------------------------------------------
ALTER TABLE items
  ADD FOREIGN KEY (user_id) REFERENCES users(user_id);

ALTER TABLE items
  ADD FOREIGN KEY (category_id) REFERENCES categories(category_id);

ALTER TABLE item_images
  ADD FOREIGN KEY (item_id) REFERENCES items(item_id);

ALTER TABLE applications
  ADD FOREIGN KEY (item_id) REFERENCES items(item_id);

ALTER TABLE applications
  ADD FOREIGN KEY (user_id) REFERENCES users(user_id);

ALTER TABLE transactions
  ADD FOREIGN KEY (item_id) REFERENCES items(item_id);

ALTER TABLE transactions
  ADD FOREIGN KEY (seller_id) REFERENCES users(user_id);

ALTER TABLE transactions
  ADD FOREIGN KEY (buyer_id) REFERENCES users(user_id);

ALTER TABLE messages
  ADD FOREIGN KEY (sender_id) REFERENCES users(user_id);

ALTER TABLE messages
  ADD FOREIGN KEY (receiver_id) REFERENCES users(user_id);

ALTER TABLE messages
  ADD FOREIGN KEY (item_id) REFERENCES items(item_id);




ALTER TABLE reports
  ADD FOREIGN KEY (user_id) REFERENCES users(user_id);
