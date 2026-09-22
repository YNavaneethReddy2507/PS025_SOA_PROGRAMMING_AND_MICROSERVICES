CREATE DATABASE IF NOT EXISTS `auth_db`;
CREATE DATABASE IF NOT EXISTS `auction_db`;
CREATE DATABASE IF NOT EXISTS `bidding_db`;
CREATE DATABASE IF NOT EXISTS `payment_db`;

-- Dedicated service users with least privilege schema ownership
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY 'auth_pass';
GRANT ALL PRIVILEGES ON `auth_db`.* TO 'auth_user'@'%';

CREATE USER IF NOT EXISTS 'auction_user'@'%' IDENTIFIED BY 'auction_pass';
GRANT ALL PRIVILEGES ON `auction_db`.* TO 'auction_user'@'%';

CREATE USER IF NOT EXISTS 'bidding_user'@'%' IDENTIFIED BY 'bidding_pass';
GRANT ALL PRIVILEGES ON `bidding_db`.* TO 'bidding_user'@'%';

CREATE USER IF NOT EXISTS 'payment_user'@'%' IDENTIFIED BY 'payment_pass';
GRANT ALL PRIVILEGES ON `payment_db`.* TO 'payment_user'@'%';

-- Root administrative access
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' IDENTIFIED BY 'rootpassword';
FLUSH PRIVILEGES;
