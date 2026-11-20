-- Like / dislike cho binh luan (giong ACTION_VIDEO nhung theo comment_id).
-- Moi user chi co 1 cam xuc tren 1 binh luan (uq_action_comment), xoa binh luan thi xoa luon cam xuc.
--   /opt/lampp/bin/mysql -u root YT < server-java/sql/003_add_comment_reactions.sql

CREATE TABLE IF NOT EXISTS `ACTION_COMMENT` (
  `id`         VARCHAR(36) NOT NULL,
  `user_id`    VARCHAR(36) NOT NULL,
  `comment_id` VARCHAR(36) NOT NULL,
  `action`     VARCHAR(8)  NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_action_comment` (`comment_id`, `user_id`),
  KEY `idx_action_comment_user` (`user_id`),
  CONSTRAINT `fk_ac_user`    FOREIGN KEY (`user_id`)    REFERENCES `USERS` (`id`)   ON DELETE CASCADE,
  CONSTRAINT `fk_ac_comment` FOREIGN KEY (`comment_id`) REFERENCES `COMMENT` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
