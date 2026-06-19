-- users (70件)
INSERT INTO users (
  user_id, name, email, password, role, department, status,
  created_at, icon_url, login_at, temporary_password, last_password_change, login_fail_count
)
VALUES
(26001, '山田太郎', 'taro@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26002, '佐藤花子', 'hanako@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26003, '鈴木健一', 'suzuki@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26004, '田中美咲', 'tanaka@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26005, '伊藤翔太', 'itou@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26006, '渡辺由美', 'watanabe@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26007, '佐藤拓也', 'satou.takuya@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26008, '中村ひかり', 'nakamura@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26009, '木村大輔', 'kimura@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26010, '高橋優子', 'takahashi@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26011, '小林隆', 'kobayashi@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26012, '加藤結衣', 'katou@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26013, '山本哲也', 'yamamoto@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26014, '林美香', 'hayashi@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26015, '清水良太', 'shimizu@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26016, '吉田美帆', 'yoshida@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26017, '佐々木翔', 'sasaki@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26018, '後藤愛菜', 'gotou@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26019, '松田圭司', 'matsuda@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26020, '石田麗子', 'ishida@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26021, '遠藤健太', 'endou@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26022, '豊田由衣', 'toyota@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26023, '斎藤勇気', 'saitou@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26024, '本田美和', 'honda@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26025, '片岡正彦', 'kataoka@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26026, '西村夏希', 'nishimura@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26027, '山崎太郎', 'yamazaki@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26028, '新垣千夏', 'aragaki@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26029, '角田康平', 'kakuta@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26030, '細谷夏音', 'hosotani@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26031, '柴田光太', 'shibata@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26032, '関晴代', 'seki@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26033, '増田恵太', 'masuda@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26034, '福島由希子', 'fukushima@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26035, '北林拓也', 'kitabayashi@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26036, '菅原百合', 'sugahara@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26037, '高西竜太', 'takanishi@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26038, '坂下友里', 'sakashita@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26039, '村田勝也', 'murata@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26040, '西野由香', 'nishino@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26041, '池田伊織', 'ikeda@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26042, '横山由紀', 'yokoyama@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26043, '相馬康二', 'soma@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26044, '瀬川紀衣', 'segawa@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26045, '大西司郎', 'ounishi@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26046, '羽田由美', 'haneda@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26047, '磯部智之', 'isobe@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26048, '小松美有', 'kommatsu@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26049, '小栗旬', 'oguri@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26050, '石橋由希', 'ishibashi@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26051, '太田光', 'oota@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26052, '角川歌子', 'kakugawa@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26053, '久保裕紀', 'kubo@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26054, '金子樹理', 'kaneko@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26055, '馬場美咲', 'baba@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26056, '神尾葉子', 'kamio@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26057, '三浦貴大', 'miura@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26058, '塚越由衣', 'tsukakoshi@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26059, '矢部浩之', 'yabe@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26060, '永田幸希', 'nagata@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26061, '黒崎えりか', 'kurosaki@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26062, '玉木宏', 'tamaki@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26063, '松村北斗', 'matsumura@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26064, '横田真悠', 'yokota@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26065, '中川大志', 'nakagawa@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26066, '福本莉子', 'fukumoto@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(26067, '辻岡正人', 'tsujioka@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26068, '須賀健太', 'suga@sample.com', 'pass', 0, '開発部', 0, now(), NULL, now(), false, now(), 0),
(26069, '武田玲奈', 'takeda@sample.com', 'pass', 0, '営業部', 0, now(), NULL, now(), false, now(), 0),
(26070, '薬丸裕英', 'yakumaru@sample.com', 'pass', 0, '企画部', 0, now(), NULL, now(), false, now(), 0),
(99999, '管理者', 'admin@sample.com', 'pass', 1, '管理部', 0, now(), NULL, now(), false, now(), 0);

-- categories（category_id は 1:IT機器 2:スマホ 3:周辺機器 4:書籍）
INSERT INTO categories (name)
VALUES ('IT機器'), ('スマホ'), ('周辺機器'), ('書籍');

-- items (40件)
-- created_at を 2026-04 / 2026-05 / 2026-06 に分散し、日別・月別の差が出るようにする。
-- category_id は 1〜4 に分散（1:IT機器 2:スマホ 3:周辺機器 4:書籍）。
-- status は表示用の目安（成約数の集計は transactions.status = 1 を使用）。
--   0:出品中 1:成約中 2:譲渡完了。item_id は登録順に 1〜40。
INSERT INTO items (
  user_id, name, description, condition, price,
  type, status, category_id, deadline, created_at, place
)
VALUES
-- ▼ 2026-04（10件）
(26001, 'ノートPC', '美品', 1, 50000, 1, 2, 1, '2026-12-31', '2026-04-01 10:00:00', 0),
(26002, 'キーボード', '新品', 0, 3000, 1, 2, 3, '2026-12-31', '2026-04-01 14:00:00', 1),
(26003, 'iPhone 13', '傷あり', 2, 40000, 1, 2, 2, '2026-12-31', '2026-04-05 09:30:00', 0),
(26004, 'ワイヤレスマウス', '新品', 0, 2000, 1, 1, 3, '2026-12-31', '2026-04-08 11:00:00', 1),
(26005, 'USB-Cケーブル', '新品', 0, 800, 1, 2, 3, '2026-12-31', '2026-04-10 16:00:00', 0),
(26006, '液晶ディスプレイ 27インチ', '良好', 1, 15000, 1, 0, 1, '2026-12-31', '2026-04-15 10:00:00', 1),
(26007, 'iPad Pro 12.9', '美品', 1, 35000, 1, 0, 2, '2026-12-31', '2026-04-18 13:00:00', 0),
(26008, 'Webカメラ', '新品未使用', 0, 5000, 1, 0, 3, '2026-12-31', '2026-04-22 15:00:00', 1),
(26009, 'ワイヤレスイヤホン', '美品', 1, 8000, 1, 0, 3, '2026-12-31', '2026-04-25 09:00:00', 0),
(26010, 'ノートパソコンスタンド', '新品', 0, 3500, 1, 0, 1, '2026-12-31', '2026-04-30 17:00:00', 1),
-- ▼ 2026-05（14件）
(26011, 'Galaxy S21', '良好', 1, 35000, 1, 2, 2, '2026-12-31', '2026-05-02 10:00:00', 0),
(26012, 'HUB機器 10ポート', '新品', 0, 4500, 1, 2, 1, '2026-12-31', '2026-05-03 11:00:00', 1),
(26013, 'Bluetooth スピーカー', '美品', 1, 6000, 1, 1, 4, '2026-12-31', '2026-05-06 14:00:00', 0),
(26014, '外付けSSD 1TB', '新品', 0, 8000, 1, 2, 1, '2026-12-31', '2026-05-08 09:00:00', 1),
(26015, 'Apple Watch Series 7', '良好', 1, 25000, 1, 2, 2, '2026-12-31', '2026-05-10 16:00:00', 0),
(26016, 'キーボード Bluetooth', '新品', 0, 4000, 1, 0, 3, '2026-12-31', '2026-05-12 10:00:00', 1),
(26017, 'モニターアーム', '新品未使用', 0, 3200, 1, 2, 1, '2026-12-31', '2026-05-15 13:00:00', 0),
(26018, 'Pixel 6', '傷あり', 2, 30000, 1, 0, 2, '2026-12-31', '2026-05-18 15:00:00', 1),
(26019, 'USB 3.0 メモリ 64GB', '新品', 0, 1500, 1, 0, 1, '2026-12-31', '2026-05-20 09:30:00', 0),
(26020, 'ワイヤレスキーボード＋マウスセット', '新品', 0, 4500, 1, 2, 3, '2026-12-31', '2026-05-22 11:00:00', 1),
(26021, 'MagSafe対応ケース', '新品未使用', 0, 2500, 1, 0, 2, '2026-12-31', '2026-05-25 14:00:00', 0),
(26022, 'USB-C PD充電器 30W', '新品', 0, 2000, 1, 0, 3, '2026-12-31', '2026-05-27 16:00:00', 1),
(26023, 'ノイズキャンセリングイヤホン', '美品', 1, 12000, 1, 0, 4, '2026-12-30', '2026-05-29 10:00:00', 0),
(26024, 'Androidタブレット', '良好', 1, 20000, 1, 0, 2, '2026-12-31', '2026-05-31 12:00:00', 1),
-- ▼ 2026-06（16件）
(26025, 'パワーバンク 20000mAh', '新品', 0, 3500, 1, 2, 1, '2026-12-31', '2026-06-01 10:00:00', 0),
(26026, 'ゲーミングマウス', '美品', 1, 5500, 1, 2, 3, '2026-12-31', '2026-06-02 11:00:00', 1),
(26027, 'USB HUB 4ポート', '新品', 0, 2500, 1, 2, 1, '2026-12-31', '2026-06-03 09:00:00', 0),
(26028, '画面保護フィルム', '新品未使用', 0, 800, 1, 2, 2, '2026-12-31', '2026-06-05 14:00:00', 1),
(26029, 'ワイヤレス充電パッド', '新品', 0, 3000, 1, 1, 1, '2026-12-31', '2026-06-07 16:00:00', 0),
(26030, 'ヘッドセット', '良好', 1, 7000, 1, 2, 3, '2026-12-31', '2026-06-08 10:00:00', 1),
(26031, 'スマートウォッチバンド', '新品', 0, 1200, 1, 0, 2, '2026-12-31', '2026-06-10 13:00:00', 0),
(26032, 'HDMI ケーブル 2m', '新品', 0, 800, 1, 2, 1, '2026-12-31', '2026-06-11 15:00:00', 1),
(26033, 'ビデオライト', '新品未使用', 0, 6000, 1, 0, 4, '2026-12-31', '2026-06-12 09:30:00', 0),
(26034, 'ポータブルプロジェクター', '美品', 1, 18000, 1, 0, 4, '2026-12-31', '2026-06-13 11:00:00', 1),
(26035, 'スマホグリップ', '新品', 0, 1000, 1, 2, 2, '2026-12-31', '2026-06-15 14:00:00', 0),
(26036, 'PCクーラー', '新品', 0, 4000, 1, 0, 1, '2026-12-31', '2026-06-16 16:00:00', 1),
(26037, 'スマホスタンド アルミ', '新品未使用', 0, 1800, 1, 0, 2, '2026-12-31', '2026-06-17 10:00:00', 0),
(26038, 'マイク USBコンデンサー', '新品', 0, 9000, 1, 2, 3, '2026-12-31', '2026-06-18 13:00:00', 1),
(26039, 'ケーブルリール', '新品', 0, 2200, 1, 0, 4, '2026-12-31', '2026-06-19 09:00:00', 0),
(26040, 'スマートプラグ', '美品', 1, 2500, 1, 0, 3, '2026-12-31', '2026-06-19 15:00:00', 1);

-- item_images (15件)
INSERT INTO item_images (item_id, image_url)
VALUES
(1, '/images/item_1_1.jpg'),
(1, '/images/item_1_2.jpg'),
(2, '/images/item_2_1.jpg'),
(3, '/images/item_3_1.jpg'),
(3, '/images/item_3_2.jpg'),
(4, '/images/item_4_1.jpg'),
(5, '/images/item_5_1.jpg'),
(6, '/images/item_6_1.jpg'),
(7, '/images/item_7_1.jpg'),
(8, '/images/item_8_1.jpg'),
(9, '/images/item_9_1.jpg'),
(10, '/images/item_10_1.jpg'),
(15, '/images/item_15_1.jpg'),
(20, '/images/item_20_1.jpg'),
(25, '/images/item_25_1.jpg');

-- applications (25件)
INSERT INTO applications (item_id, user_id, bid_price, status, created_at)
VALUES
(1, 26002, 48000, 0, now()),
(1, 26004, 49000, 0, now()),
(2, 26001, 3000, 0, now()),
(3, 26005, 38000, 0, now()),
(3, 26007, 39500, 0, now()),
(4, 26003, 2000, 0, now()),
(5, 26008, 800, 0, now()),
(6, 26009, 14500, 0, now()),
(7, 26010, 34000, 0, now()),
(8, 26011, 4800, 0, now()),
(9, 26012, 7500, 0, now()),
(10, 26013, 3400, 0, now()),
(11, 26014, 33000, 0, now()),
(12, 26015, 4200, 0, now()),
(13, 26016, 5700, 0, now()),
(14, 26017, 7600, 0, now()),
(15, 26018, 24000, 0, now()),
(16, 26019, 3900, 0, now()),
(17, 26020, 2900, 0, now()),
(18, 26021, 29000, 0, now()),
(19, 26022, 1400, 0, now()),
(20, 26023, 11500, 0, now()),
(21, 26024, 2400, 0, now()),
(22, 26025, 1900, 0, now()),
(23, 26026, 5200, 0, now());

-- transactions (21件)
-- 成約数の集計は status = 1 で行う（18件）。比較用に status = 0（進行中）を3件入れる。
-- item_id は上の items の登録順（1〜40）を参照。seller_id は出品者、buyer_id は別の社員。
-- status = 1 は items.created_at と同じ月内の completed_at を設定（譲渡完了の証跡）。
INSERT INTO transactions (
  item_id, seller_id, buyer_id, status, completed_at
)
VALUES
-- ▼ 成約済み（status = 1）2026-04
(1,  26001, 26042, 1, '2026-04-03 15:00:00'),
(2,  26002, 26043, 1, '2026-04-04 12:00:00'),
(3,  26003, 26044, 1, '2026-04-08 10:00:00'),
(5,  26005, 26046, 1, '2026-04-13 16:00:00'),
-- ▼ 成約済み（status = 1）2026-05
(11, 26011, 26052, 1, '2026-05-05 11:00:00'),
(12, 26012, 26053, 1, '2026-05-06 14:00:00'),
(14, 26014, 26055, 1, '2026-05-11 09:00:00'),
(15, 26015, 26056, 1, '2026-05-13 16:00:00'),
(17, 26017, 26058, 1, '2026-05-18 13:00:00'),
(20, 26020, 26041, 1, '2026-05-25 11:00:00'),
-- ▼ 成約済み（status = 1）2026-06
(25, 26025, 26046, 1, '2026-06-04 10:00:00'),
(26, 26026, 26047, 1, '2026-06-05 11:00:00'),
(27, 26027, 26048, 1, '2026-06-06 09:00:00'),
(28, 26028, 26049, 1, '2026-06-08 14:00:00'),
(30, 26030, 26051, 1, '2026-06-11 10:00:00'),
(32, 26032, 26053, 1, '2026-06-14 15:00:00'),
(35, 26035, 26056, 1, '2026-06-18 14:00:00'),
(38, 26038, 26059, 1, '2026-06-19 13:00:00'),
-- ▼ 進行中（status = 0）成約数には含めない
(4,  26004, 26045, 0, NULL),
(13, 26013, 26054, 0, NULL),
(29, 26029, 26050, 0, NULL);

-- messages (20件)
INSERT INTO messages (
  sender_id, receiver_id, item_id, content, created_at
)
VALUES
(26002, 26001, 1, '購入希望です', now()),
(26001, 26002, 1, 'ありがとうございます。ご購入ください。', now()),
(26001, 26002, 2, 'こちらのキーボード、いかがですか？', now()),
(26005, 26003, 3, 'iPhone 13、購入を検討しています', now()),
(26003, 26005, 3, '傷がありますが、動作は完璧です', now()),
(26008, 26005, 5, 'ケーブル、在庫ありますか？', now()),
(26005, 26008, 5, 'はい、在庫あります。すぐに発送できます', now()),
(26009, 26006, 6, 'ディスプレイ、配送は可能ですか？', now()),
(26006, 26009, 6, '配送可能です。送料は着払いでお願いします', now()),
(26010, 26007, 7, 'iPad Pro、傷や汚れはありませんか？', now()),
(26007, 26010, 7, '美品です。ほぼ未使用に近い状態です', now()),
(26011, 26008, 8, 'Webカメラ、画質はどうですか？', now()),
(26008, 26011, 8, '1080p、フルHDで綺麗です', now()),
(26012, 26009, 9, 'イヤホン、何時間もちますか？', now()),
(26009, 26012, 9, 'バッテリーは最大8時間です', now()),
(26013, 26010, 10, 'PCスタンド、何kg対応ですか？', now()),
(26010, 26013, 10, '最大15kg対応です。ノートPCに最適です', now()),
(26014, 26011, 11, 'Galaxy S21、SIMロックはありますか？', now()),
(26011, 26014, 11, 'SIMロックなし、自由に使用できます', now()),
(26015, 26012, 12, 'HUB機器、USB 3.0は何ポートですか？', now());

-- ng_keywords
INSERT INTO ng_keywords (keyword, type, status, created_at)
VALUES
('禁止ワード',2,0,now()),
('不適切',1,0,now());

-- search_keywords
INSERT INTO search_keywords (keyword, status, created_at)
VALUES
('ノートPC', 0, now()),
('スマホ', 0, now()),
('キーボード', 0, now()),
('ディスプレイ', 0, now()),
('イヤホン', 0, now());



-- reports（通報：動作確認用サンプル）
INSERT INTO reports (target_type, target_id, user_id, reason, status, created_at)
VALUES
(0, 3, 26005, '商品説明と実物が異なる可能性があります', 0, now()),
(1, 1, 26002, '不適切な表現が含まれています', 0, now()),
(2, 7, 26010, '受け渡し場所で連絡が取れなくなりました', 1, now());

-- 既存サンプル取引へ譲渡IDを付与
UPDATE transactions
SET transfer_code = 'sample_transfer_' || transaction_id
WHERE transfer_code IS NULL;
