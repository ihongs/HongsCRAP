-- DB: mesage

--
-- 容器
--

DROP TABLE IF EXISTS `a_mesage_context`;
CREATE TABLE `a_mesage_context` (
  `id` CHAR(14) NOT NULL,
  `name` VARCHAR(200) DEFAULT NULL,
  `note` VARCHAR(500) DEFAULT NULL,
  `token` CHAR(14) NOT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  `mtime` INTEGER UNSIGNED DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`),
);

CREATE INDEX `IK_a_mesage_content_state` ON `a_mesage_content` (`state`);

--
-- 消息
--

DROP TABLE IF EXISTS `a_mesage_content`;
CREATE TABLE `a_mesage_content` (
  `id` CHAR(14) NOT NULL,
  `room_id` CHAR(14) NOT NULL,
  `mate_id` CHAR(20) NOT NULL,
  `kind` VARCHAR(10) DEFAULT 'text',
  `note` TEXT DEFAULT NULL,
  `data` TEXT DEFAULT NULL,
  `ctime` INTEGER UNSIGNED DEFAULT NULL,
  `state` TINYINT DEFAULT '1',
  PRIMARY KEY (`id`)
);

CREATE INDEX `IK_a_mesage_content_room` ON `a_mesage_content` (`room_id`);
CREATE INDEX `IK_a_mesage_content_mate` ON `a_mesage_content` (`mate_id`);
CREATE INDEX `IK_a_mesage_content_ctime` ON `a_mesage_content` (`ctime`);
CREATE INDEX `IK_a_mesage_content_state` ON `a_mesage_content` (`state`);
