-- ============================================================
-- Migration: thay cot VIDEO.type bang he thong tag (TAG + VIDEO_TAG)
-- Chay tren DB dang co du lieu (KHONG xoa du lieu):
--   mysql -u root YT < server-java/sql/001_add_tags.sql
-- Chay lai nhieu lan cung khong sao.
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `TAG` (
  `id` varchar(36) NOT NULL,
  `name` varchar(30) NOT NULL,
  `slug` varchar(40) NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_tag_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE IF NOT EXISTS `VIDEO_TAG` (
  `video_id` varchar(36) NOT NULL,
  `tag_id` varchar(36) NOT NULL,
  PRIMARY KEY (`video_id`,`tag_id`),
  KEY `idx_video_tag_tag` (`tag_id`),
  CONSTRAINT `fk_vt_video` FOREIGN KEY (`video_id`) REFERENCES `VIDEO` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_vt_tag` FOREIGN KEY (`tag_id`) REFERENCES `TAG` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Chuyen gia tri type cu (horror, action, ...) thanh tag, bo qua 'other' va gia tri rong
INSERT IGNORE INTO `TAG` (id, name, slug)
SELECT UUID(), t.type, t.type
FROM (
  SELECT DISTINCT LOWER(TRIM(type)) AS type
  FROM `VIDEO`
  WHERE type IS NOT NULL AND TRIM(type) <> '' AND LOWER(TRIM(type)) <> 'other'
) AS t;

INSERT IGNORE INTO `VIDEO_TAG` (video_id, tag_id)
SELECT v.id, tg.id
FROM `VIDEO` v
JOIN `TAG` tg ON tg.slug = LOWER(TRIM(v.type));

-- Code khong con dung cot type nua. Kiem tra tag da chuyen dung roi thi co the xoa:
-- ALTER TABLE `VIDEO` DROP COLUMN `type`;
