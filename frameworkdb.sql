SET NAMES utf8;

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
  `sessid` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `expires` int(255) NOT NULL,
  `data` text COLLATE utf8_unicode_ci NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;


DROP TABLE IF EXISTS `sites`;
CREATE TABLE `sites` (
  `siteID` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'Unnamed Chiori Framework Site',
  `domain` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `subdomains` text COLLATE utf8_unicode_ci NOT NULL,
  `protected` text COLLATE utf8_unicode_ci NOT NULL,
  `metatags` text COLLATE utf8_unicode_ci NOT NULL,
  `aliases` text COLLATE utf8_unicode_ci NOT NULL,
  `configYaml` text COLLATE utf8_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
