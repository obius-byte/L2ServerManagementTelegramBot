/*
 Navicat Premium Data Transfer

 Source Server         : 192.168.0.200
 Source Server Type    : MariaDB
 Source Server Version : 100148
 Source Host           : 127.0.0.1:3306
 Source Schema         : l2jdb

 Target Server Type    : MariaDB
 Target Server Version : 100148
 File Encoding         : 65001

 Date: 24/08/2023 23:13:23
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for delayed_tasks
-- ----------------------------
DROP TABLE IF EXISTS `delayed_tasks`;
CREATE TABLE `delayed_tasks`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `event` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '',
  `args` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Compact;

SET FOREIGN_KEY_CHECKS = 1;
