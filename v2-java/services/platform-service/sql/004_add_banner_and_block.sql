-- Anh bia trang ca nhan + danh sach "khong de xuat video cua nguoi nay".
-- Bao cao user dung lai bang REPORTS co san (reported_user, reporter, reason, detail).
--   /opt/lampp/bin/mysql -u root YT < server-java/sql/004_add_banner_and_block.sql

ALTER TABLE `USERS` ADD COLUMN IF NOT EXISTS `banner` VARCHAR(100) DEFAULT NULL AFTER `avatar`;

-- Moi cap (nguoi chan, nguoi bi chan) chi 1 dong, xoa user thi xoa luon
CREATE TABLE IF NOT EXISTS `BLOCKED_USERS` (
  `user_id`    VARCHAR(36) NOT NULL,
  `blocked_id` VARCHAR(36) NOT NULL,
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `blocked_id`),
  KEY `idx_blocked_users_blocked` (`blocked_id`),
  CONSTRAINT `fk_blocked_users_user`    FOREIGN KEY (`user_id`)    REFERENCES `USERS` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_blocked_users_blocked` FOREIGN KEY (`blocked_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
