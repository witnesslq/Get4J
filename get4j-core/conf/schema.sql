-- SQL SCRIPT FOR MYSQL

CREATE DATABASE IF NOT EXISTS `get4j`
  DEFAULT CHARACTER SET utf8
  COLLATE utf8_general_ci;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `page`;
CREATE TABLE `page` (
  `ID`            VARCHAR(50)   NOT NULL PRIMARY KEY
  COMMENT '主键uuid',
  `SEED_NAME`     VARCHAR(50) COMMENT '种子名称',
  `SITE_HOST`     VARCHAR(2048) NOT NULL
  COMMENT '平台站点地址',
  `FETCH_URL`     VARCHAR(2048) NOT NULL
  COMMENT '每次抓取的url地址',
  `TITLE`         VARCHAR(1000) COMMENT '网页标题',
  `FETCH_TIME`    VARCHAR(20) COMMENT '抓取时间',
  `FETCH_CONTENT` LONGTEXT COMMENT 'Html/Json/Xml文件内容',
  `RESOURCES_URL` LONGTEXT COMMENT '网页内容中的资源文件Url',
  `COOKIES`       VARCHAR(2048) COMMENT 'Response COOKIES',
  `AVATAR`        VARCHAR(1024) COMMENT '头像存放路径',
  `CREATE_TIME`   DATETIME COMMENT '创建日期',
  `UPDATE_TIME`   DATETIME COMMENT '更新日期'
)
  ENGINE = INNODB
  DEFAULT CHARSET = utf8;

-- MongoDB Command

USE get4j
db.page.drop()
db.createCollection("page")
db.page.ensureIndex({"fetch_url":1}, {"unique":TRUE})