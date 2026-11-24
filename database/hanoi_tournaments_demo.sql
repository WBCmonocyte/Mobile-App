-- EasySport: ba giai dau gia lap. Thoi gian DATETIME theo gio Viet Nam.
-- Chon schema cua ung dung trong Workbench truoc khi chay.
SET NAMES utf8mb4;

-- Dung cung charset va collation voi courts.id tren chinh database dich.
SELECT CHARACTER_SET_NAME, COLLATION_NAME INTO @court_charset, @court_collation
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'courts' AND COLUMN_NAME = 'id';

SET @ddl = CONCAT('CREATE TABLE IF NOT EXISTS tournaments (
    id VARCHAR(50) PRIMARY KEY,
    court_id VARCHAR(50) CHARACTER SET ', @court_charset, ' COLLATE ', @court_collation, ' NOT NULL,
    title VARCHAR(160) NOT NULL,
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    max_teams SMALLINT UNSIGNED NOT NULL,
    is_demo BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_tournaments_start (starts_at),
    CONSTRAINT fk_tournaments_court FOREIGN KEY (court_id) REFERENCES courts(id),
    CHECK (ends_at > starts_at),
    CHECK (max_teams > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4');
PREPARE create_tournaments FROM @ddl;
EXECUTE create_tournaments;
DEALLOCATE PREPARE create_tournaments;

START TRANSACTION;
INSERT INTO tournaments (id, court_id, title, starts_at, ends_at, max_teams, is_demo)
VALUES
('demo_hc_dec26', 'hanoi_fb_hoang_cau', 'Cúp bóng đá 7 người Hoàng Cầu', '2026-12-05 08:00', '2026-12-05 17:00', 8, TRUE),
('demo_tl_dec26', 'hanoi_fb_thuy_loi', 'Giải giao hữu sinh viên Thủy Lợi', '2026-12-06 07:30', '2026-12-06 17:30', 8, TRUE),
('demo_yhn_dec26', 'hanoi_fb_y_ha_noi', 'Ngày hội bóng đá sinh viên Hà Nội', '2026-12-12 08:00', '2026-12-12 17:00', 6, TRUE)
ON DUPLICATE KEY UPDATE id = tournaments.id;
COMMIT;

SELECT t.title, c.name AS court_name, c.address, t.starts_at, t.ends_at, t.max_teams, t.is_demo
FROM tournaments t JOIN courts c ON c.id = t.court_id
WHERE t.id IN ('demo_hc_dec26','demo_tl_dec26','demo_yhn_dec26')
ORDER BY t.starts_at;
