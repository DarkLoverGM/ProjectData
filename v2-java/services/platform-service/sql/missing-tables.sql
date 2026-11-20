-- 3 bang ma server-java can nhung DB `YT` chua co.
-- Ben Node cac du lieu nay dang nam o file JSON (server/data/log.json, reports.json)
-- va cot followers/following (chua ton tai) tren bang USERS.
--
-- Chay thu cong khi can:
--   mysql -u root YT < server-java/sql/missing-tables.sql
--
-- Server van khoi dong binh thuong khi chua chay file nay; chi cac endpoint
-- /api/track, /api/log, /api/report la loi.

CREATE TABLE IF NOT EXISTS `LOGS` (
  `id`         VARCHAR(36)  NOT NULL,
  `ip`         VARCHAR(45)  DEFAULT NULL,
  `user_agent` VARCHAR(255) DEFAULT NULL,
  `referrer`   VARCHAR(255) DEFAULT NULL,
  `timestamp`  DATETIME     DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `REPORTS` (
  `id`            VARCHAR(36)  NOT NULL,
  `comment_id`    VARCHAR(36)  DEFAULT NULL,
  `reported_user` VARCHAR(100) DEFAULT NULL,
  `reporter`      VARCHAR(100) DEFAULT NULL,
  `reason`        VARCHAR(255) DEFAULT NULL,
  `detail`        TEXT,
  `created_at`    DATETIME     DEFAULT NULL,
  `status`        VARCHAR(50)  DEFAULT 'pending',
  PRIMARY KEY (`id`),
  KEY `idx_reports_comment` (`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `FOLLOWERS` (
  `id`           VARCHAR(36) NOT NULL,
  `follower_id`  VARCHAR(36) NOT NULL,
  `following_id` VARCHAR(36) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_follow_pair` (`follower_id`, `following_id`),
  KEY `idx_followers_following` (`following_id`),
  CONSTRAINT `fk_followers_follower`  FOREIGN KEY (`follower_id`)  REFERENCES `USERS` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_followers_following` FOREIGN KEY (`following_id`) REFERENCES `USERS` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
