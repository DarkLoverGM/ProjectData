-- Cot luu danh sach user bi chan (mang JSON cac id), dung boi /api/blockUser/:id
-- va de loc video o /api/videos/all. DB YT hien chua co cot nay nen 2 endpoint chan/bo chan tra 500.
--   /opt/lampp/bin/mysql -u root YT < server-java/sql/002_add_blocked_users.sql

ALTER TABLE `USERS` ADD COLUMN IF NOT EXISTS `blocked_users` TEXT DEFAULT NULL;
