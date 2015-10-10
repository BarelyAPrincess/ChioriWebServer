SET NAMES utf8;
SET foreign_key_checks = 0;
SET time_zone = '-05:00';
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

CREATE DATABASE `chiorifw` /*!40100 DEFAULT CHARACTER SET latin1 */;
USE `chiorifw`;

DROP TABLE IF EXISTS `pages`;
CREATE TABLE `pages` (
  `site` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `domain` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `page` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `reqlevel` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '-1',
  `theme` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `view` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `html` text COLLATE utf8_unicode_ci NOT NULL,
  `file` varchar(255) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

DROP TABLE IF EXISTS `sessions`;
CREATE TABLE `sessions` (
  `sessionId` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `sessionName` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `ipAddr` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `sessionSite` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT '',
  `timeout` int(255) NOT NULL,
  `data` text COLLATE utf8_unicode_ci NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

DROP TABLE IF EXISTS `sites`;
CREATE TABLE `sites` (
  `siteID` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'Unnamed Chiori Framework Site',
  `domain` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `source` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'pages',
  `resource` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'resources',
  `subdomains` text COLLATE utf8_unicode_ci NOT NULL,
  `protected` text COLLATE utf8_unicode_ci NOT NULL,
  `metatags` text COLLATE utf8_unicode_ci NOT NULL,
  `aliases` text COLLATE utf8_unicode_ci NOT NULL,
  `configYaml` text COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `email` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `username` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `password` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `fname` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `acctId` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `imgID` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `address` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `city` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `state` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `zipcode` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `country` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `phone` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `sex` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `date` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `numloginfail` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `lastloginfail` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `timezone` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `type` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `actnum` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `userlevel` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `lastactive` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `lastlogin` varchar(255) COLLATE utf8_unicode_ci NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
