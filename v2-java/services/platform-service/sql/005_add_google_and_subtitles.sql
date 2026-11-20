-- Dang nhap Google (USERS.google_id) + phu de video (SUBTITLE).
-- Ben Node (MongoDB) la field users.google_id va collection subtitles.
--   /opt/lampp/bin/mysql -u root YT < server-java/sql/005_add_google_and_subtitles.sql

-- Tai khoan tao bang Google co password NULL (dat sau bang Quen mat khau)
ALTER TABLE `USERS` ADD COLUMN IF NOT EXISTS `google_id` VARCHAR(64) DEFAULT NULL AFTER `password`;
CREATE UNIQUE INDEX IF NOT EXISTS `uq_users_google_id` ON `USERS` (`google_id`);
-- Avatar Google la URL day du (https://lh3.googleusercontent.com/...), dai hon 100 ky tu
ALTER TABLE `USERS` MODIFY `avatar` VARCHAR(500) DEFAULT NULL;

-- Moi video toi da 1 ban phu de cho moi ngon ngu, xoa video thi xoa luon phu de.
-- cues: mang JSON [{ start, end, text }] (giay), da sap theo thoi gian bat dau
CREATE TABLE IF NOT EXISTS `SUBTITLE` (
  `video_id`   VARCHAR(36) NOT NULL,
  `lang`       VARCHAR(8)  NOT NULL,
  `cues`       LONGTEXT    NOT NULL,
  `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_by` VARCHAR(36) DEFAULT NULL,
  PRIMARY KEY (`video_id`, `lang`),
  CONSTRAINT `fk_subtitle_video` FOREIGN KEY (`video_id`) REFERENCES `VIDEO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
