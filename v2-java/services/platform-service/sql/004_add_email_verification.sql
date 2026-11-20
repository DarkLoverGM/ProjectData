-- Ma xac minh email khi dang ky (chua co user nen khong dung chung USER_RESETPASS duoc).
-- Moi email chi co 1 ma hieu luc (PK = email), dang ky xong thi xoa.
--   /opt/lampp/bin/mysql -u root YT < server-java/sql/004_add_email_verification.sql

CREATE TABLE IF NOT EXISTS `EMAIL_VERIFICATION` (
  `email`     VARCHAR(255) NOT NULL,
  `code_hash` VARCHAR(255) NOT NULL,
  `attempts`  INT(11)      NOT NULL DEFAULT 0,
  `createAt`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expiresAt` DATETIME     NOT NULL,
  PRIMARY KEY (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
