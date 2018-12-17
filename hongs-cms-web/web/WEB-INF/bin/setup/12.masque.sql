-- DB: masque

--
-- 站点
--

DROP TABLE IF EXISTS `a_masque_site`;
CREATE TABLE `a_masque_site` (
  `id` CHAR(14) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `token` CHAR(32) NOT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  `mtime` INTEGER UNSIGNED DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
);

CREATE INDEX `IK_a_masque_site_state` ON `a_masque_site` (`state`);

--
-- 消息
--

DROP TABLE IF EXISTS `a_masque_chat`;
CREATE TABLE `a_masque_chat` (
  `id` CHAR(14) NOT NULL,
  `site_id` CHAR(14) NOT NULL,
  `room_id` VARCHAR(32) NOT NULL,
  `mate_id` VARCHAR(32) NOT NULL,
  `kind` VARCHAR(10) DEFAULT 'text',
  `note` TEXT DEFAULT NULL,
  `data` TEXT DEFAULT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_chat_site` ON `a_masque_chat` (`site_id`);
CREATE INDEX `IK_a_masque_chat_room` ON `a_masque_chat` (`room_id`);
CREATE INDEX `IK_a_masque_chat_mate` ON `a_masque_chat` (`mate_id`);
CREATE INDEX `IK_a_masque_chat_ctime` ON `a_masque_chat` (`ctime`);
CREATE INDEX `IK_a_masque_chat_state` ON `a_masque_chat` (`state`);

--
-- 计数
--

DROP TABLE IF EXISTS `a_masque_stat`;
CREATE TABLE `a_masque_stat` (
  `id` CHAR(14) NOT NULL,
  `site_id` CHAR(14) NOT NULL,
  `room_id` VARCHAR(32) NOT NULL,
  `mate_id` VARCHAR(32) NOT NULL,
  `fresh` INTEGER UNSIGNED DEFAULT 0,
  `mtime` INTEGER UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE UNIQUE INDEX `UK_a_tattle_stat_mate` ON `a_tattle_stat` (`site_id`,`room_id`,`mate_id`);
CREATE INDEX `IK_a_masque_stat_site` ON `a_masque_chat` (`site_id`);
CREATE INDEX `IK_a_masque_stat_room` ON `a_masque_chat` (`room_id`);
CREATE INDEX `IK_a_masque_stat_mate` ON `a_masque_chat` (`mate_id`);

--
-- 成员
--

DROP TABLE IF EXISTS `a_masque_room_mate`;
CREATE TABLE `a_masque_mate` (
  `id` CHAR(14) NOT NULL,
  `site_id` CHAR(14) NOT NULL,
  `room_id` VARCHAR(32) NOT NULL,
  `mate_id` VARCHAR(32) NOT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_room_mate_site` ON `a_masque_room_mate` (`site_id`);
CREATE INDEX `IK_a_masque_room_mate_room` ON `a_masque_room_mate` (`room_id`);
CREATE INDEX `IK_a_masque_room_mate_mate` ON `a_masque_room_mate` (`mate_id`);
CREATE INDEX `IK_a_masque_room_mate_state` ON `a_masque_room_mate` (`state`);

--
-- 房间
--

DROP TABLE IF EXISTS `a_masque_room`;
CREATE TABLE `a_masque_room` (
  `id` CHAR(14) NOT NULL,
  `site_id` CHAR(14) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  `mtime` INTEGER UNSIGNED DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_room_site` ON `a_masque_room` (`site_id`);
CREATE INDEX `IK_a_masque_room_state` ON `a_masque_room` (`state`);

--
-- 人员
--

DROP TABLE IF EXISTS `a_masque_mate`;
CREATE TABLE `a_masque_mate` (
  `id` CHAR(14) NOT NULL,
  `site_id` CHAR(14) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `icon` VARCHAR(100) DEFAULT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  `mtime` INTEGER UNSIGNED DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`site_id`) REFERENCES `a_masque_site` (`id`) ON DELETE CASCADE
);

CREATE INDEX `IK_a_masque_mate_site` ON `a_masque_mate` (`site_id`);
CREATE INDEX `IK_a_masque_mate_state` ON `a_masque_mate` (`state`);
