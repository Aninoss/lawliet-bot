-- MySQL dump 10.13  Distrib 8.0.23, for Linux (x86_64)
--
-- Host: localhost    Database: Lawliet
-- ------------------------------------------------------
-- Server version	8.0.23

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Current Database: `Lawliet`
--

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `Lawliet` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `Lawliet`;

--
-- Table structure for table `AutoChannel`
--

DROP TABLE IF EXISTS `AutoChannel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AutoChannel` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned DEFAULT NULL,
  `active` tinyint(1) NOT NULL,
  `channelName` varchar(50) NOT NULL,
  `locked` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `AutoChannelServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AutoChannelChildChannels`
--

DROP TABLE IF EXISTS `AutoChannelChildChannels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AutoChannelChildChannels` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`channelId`),
  CONSTRAINT `AutoChannelChildChannelsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AutoClaim`
--

DROP TABLE IF EXISTS `AutoClaim`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AutoClaim` (
  `userId` bigint unsigned NOT NULL,
  `active` tinyint unsigned NOT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AutoQuote`
--

DROP TABLE IF EXISTS `AutoQuote`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AutoQuote` (
  `serverId` bigint unsigned NOT NULL,
  `active` tinyint unsigned NOT NULL,
  PRIMARY KEY (`serverId`),
  CONSTRAINT `AutoQuoteServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `AutoWork`
--

DROP TABLE IF EXISTS `AutoWork`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `AutoWork` (
  `userId` bigint unsigned NOT NULL,
  `active` tinyint unsigned NOT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BannedUsers`
--

DROP TABLE IF EXISTS `BannedUsers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BannedUsers` (
  `userId` bigint unsigned NOT NULL,
  `reason` varchar(2000) DEFAULT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BannedWords`
--

DROP TABLE IF EXISTS `BannedWords`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BannedWords` (
  `serverId` bigint unsigned NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `strict` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `BannedWordsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BannedWordsIgnoredUsers`
--

DROP TABLE IF EXISTS `BannedWordsIgnoredUsers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BannedWordsIgnoredUsers` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`userId`),
  KEY `BannedWordsIgnoredUsersUserBase` (`userId`),
  CONSTRAINT `BannedWordsIgnoredUsersServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BannedWordsLogRecievers`
--

DROP TABLE IF EXISTS `BannedWordsLogRecievers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BannedWordsLogRecievers` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`userId`),
  KEY `BannedWordsLogRecieversUserBase` (`userId`),
  CONSTRAINT `BannedWordsLogRecieversServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BannedWordsWords`
--

DROP TABLE IF EXISTS `BannedWordsWords`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BannedWordsWords` (
  `serverId` bigint unsigned NOT NULL,
  `word` varchar(20) NOT NULL,
  PRIMARY KEY (`serverId`,`word`),
  CONSTRAINT `BannedWordsWordsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `BasicRole`
--

DROP TABLE IF EXISTS `BasicRole`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `BasicRole` (
  `serverId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`roleId`),
  CONSTRAINT `RoleServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Bump`
--

DROP TABLE IF EXISTS `Bump`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Bump` (
  `next` timestamp NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CMChannels`
--

DROP TABLE IF EXISTS `CMChannels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CMChannels` (
  `serverId` bigint unsigned NOT NULL,
  `element` varchar(20) NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`element`,`channelId`),
  CONSTRAINT `CMChannelsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CMOff`
--

DROP TABLE IF EXISTS `CMOff`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CMOff` (
  `serverId` bigint unsigned NOT NULL,
  `element` varchar(50) NOT NULL,
  PRIMARY KEY (`serverId`,`element`),
  CONSTRAINT `CMGeneralServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `CommandUsages`
--

DROP TABLE IF EXISTS `CommandUsages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `CommandUsages` (
  `command` varchar(20) NOT NULL,
  `usages` int NOT NULL DEFAULT '1',
  PRIMARY KEY (`command`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `DServer`
--

DROP TABLE IF EXISTS `DServer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `DServer` (
  `serverId` bigint unsigned NOT NULL,
  `prefix` char(5) DEFAULT 'L.',
  `locale` char(7) DEFAULT 'en_us',
  `powerPlant` enum('STOPPED','PAUSED','ACTIVE','') NOT NULL DEFAULT 'STOPPED',
  `powerPlantSingleRole` tinyint(1) NOT NULL DEFAULT '0',
  `powerPlantAnnouncementChannelId` bigint unsigned DEFAULT NULL,
  `powerPlantTreasureChests` tinyint(1) NOT NULL DEFAULT '1',
  `powerPlantReminders` tinyint(1) NOT NULL DEFAULT '1',
  `powerPlantRoleMin` bigint unsigned NOT NULL DEFAULT '50000',
  `powerPlantRoleMax` bigint unsigned NOT NULL DEFAULT '800000000',
  `powerPlantVCHoursCap` int unsigned DEFAULT NULL,
  `commandAuthorMessageRemove` tinyint(1) NOT NULL DEFAULT '0',
  `fisheryCoinsGivenLimit` tinyint(1) NOT NULL DEFAULT '0',
  `big` tinyint(1) NOT NULL DEFAULT '0',
  `kicked` date DEFAULT NULL,
  PRIMARY KEY (`serverId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Donators`
--

DROP TABLE IF EXISTS `Donators`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Donators` (
  `userId` bigint unsigned NOT NULL,
  `end` date NOT NULL,
  `totalDollars` double unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FeatureRequestBoosts`
--

DROP TABLE IF EXISTS `FeatureRequestBoosts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FeatureRequestBoosts` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `boostDatetime` timestamp NOT NULL,
  `boostUserId` bigint unsigned NOT NULL,
  PRIMARY KEY (`id`,`boostDatetime`,`boostUserId`),
  CONSTRAINT `FeatureRequestsBase` FOREIGN KEY (`id`) REFERENCES `FeatureRequests` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=742 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `FeatureRequests`
--

DROP TABLE IF EXISTS `FeatureRequests`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `FeatureRequests` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `userId` bigint unsigned NOT NULL,
  `date` date NOT NULL,
  `type` enum('PENDING','COMPLETED','REJECTED') NOT NULL,
  `public` tinyint unsigned NOT NULL DEFAULT '0',
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=762 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `GameStatistics`
--

DROP TABLE IF EXISTS `GameStatistics`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `GameStatistics` (
  `game` varchar(10) NOT NULL,
  `won` tinyint(1) NOT NULL,
  `value` double NOT NULL,
  PRIMARY KEY (`game`,`won`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Giveaways`
--

DROP TABLE IF EXISTS `Giveaways`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Giveaways` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  `messageId` bigint unsigned NOT NULL,
  `emoji` varchar(100) NOT NULL,
  `winners` tinyint NOT NULL DEFAULT '0',
  `start` timestamp NOT NULL,
  `durationMinutes` mediumint unsigned NOT NULL DEFAULT '0',
  `title` varchar(250) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `imageUrl` varchar(500) DEFAULT NULL,
  `active` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`messageId`),
  KEY `GiveawaysServerBase` (`serverId`),
  CONSTRAINT `GiveawaysServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InviteTracking`
--

DROP TABLE IF EXISTS `InviteTracking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `InviteTracking` (
  `serverId` bigint unsigned NOT NULL,
  `active` tinyint unsigned NOT NULL,
  `channelId` bigint unsigned DEFAULT NULL,
  `ping` tinyint unsigned NOT NULL,
  `advanced` tinyint unsigned NOT NULL DEFAULT '1',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `InviteTrackingServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `InviteTypeUsages`
--

DROP TABLE IF EXISTS `InviteTypeUsages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `InviteTypeUsages` (
  `type` varchar(50) NOT NULL,
  `usages` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Invites`
--

DROP TABLE IF EXISTS `Invites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Invites` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `invitedByUserId` bigint unsigned NOT NULL,
  `date` date NOT NULL,
  `lastMessage` date NOT NULL,
  `fakeInvite` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`,`userId`),
  CONSTRAINT `InvitesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `JailRemainRoles`
--

DROP TABLE IF EXISTS `JailRemainRoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `JailRemainRoles` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`userId`,`roleId`),
  CONSTRAINT `JailRemainRolesJailsBase` FOREIGN KEY (`serverId`, `userId`) REFERENCES `Jails` (`serverId`, `userId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `JailRoles`
--

DROP TABLE IF EXISTS `JailRoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `JailRoles` (
  `serverId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`roleId`),
  CONSTRAINT `JailRolesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Jails`
--

DROP TABLE IF EXISTS `Jails`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Jails` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `expires` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`serverId`,`userId`),
  CONSTRAINT `JailsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `MemberCountDisplays`
--

DROP TABLE IF EXISTS `MemberCountDisplays`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `MemberCountDisplays` (
  `serverId` bigint unsigned NOT NULL,
  `vcId` bigint unsigned NOT NULL,
  `name` char(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`serverId`,`vcId`),
  CONSTRAINT `MemberStatsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Moderation`
--

DROP TABLE IF EXISTS `Moderation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Moderation` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned DEFAULT NULL,
  `question` tinyint(1) NOT NULL DEFAULT '0',
  `muteRoleId` bigint unsigned DEFAULT NULL,
  `enforceMuteRole` tinyint(1) NOT NULL DEFAULT '0',
  `autoKick` int unsigned NOT NULL DEFAULT '0',
  `autoBan` int unsigned NOT NULL DEFAULT '0',
  `autoMute` int unsigned NOT NULL DEFAULT '0',
  `autoJail` int unsigned NOT NULL DEFAULT '0',
  `autoKickDays` int unsigned NOT NULL DEFAULT '30',
  `autoBanDays` int unsigned NOT NULL DEFAULT '30',
  `autoMuteDays` int unsigned NOT NULL DEFAULT '30',
  `autoJailDays` int unsigned NOT NULL DEFAULT '30',
  `autoBanDuration` int unsigned NOT NULL DEFAULT '0',
  `autoMuteDuration` int unsigned NOT NULL DEFAULT '0',
  `autoJailDuration` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `ModerationServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `NSFWFilter`
--

DROP TABLE IF EXISTS `NSFWFilter`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `NSFWFilter` (
  `serverId` bigint unsigned NOT NULL,
  `keyword` char(50) NOT NULL DEFAULT '',
  PRIMARY KEY (`serverId`,`keyword`),
  CONSTRAINT `NSFWFilterServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `OsuAccounts`
--

DROP TABLE IF EXISTS `OsuAccounts`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `OsuAccounts` (
  `userId` bigint unsigned NOT NULL,
  `osuId` bigint unsigned NOT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PaddleSubscriptions`
--

DROP TABLE IF EXISTS `PaddleSubscriptions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PaddleSubscriptions` (
  `subId` bigint unsigned NOT NULL DEFAULT '0',
  `userId` bigint unsigned NOT NULL DEFAULT '0',
  `unlocksServer` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`subId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Patreon`
--

DROP TABLE IF EXISTS `Patreon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Patreon` (
  `userId` bigint unsigned NOT NULL,
  `tier` tinyint NOT NULL,
  `expires` date NOT NULL,
  PRIMARY KEY (`userId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PatreonOld`
--

DROP TABLE IF EXISTS `PatreonOld`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PatreonOld` (
  `userId` bigint unsigned NOT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PowerPlantIgnoredChannels`
--

DROP TABLE IF EXISTS `PowerPlantIgnoredChannels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PowerPlantIgnoredChannels` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`channelId`),
  CONSTRAINT `PowerPlantIgnoredChannelsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `PowerPlantRoles`
--

DROP TABLE IF EXISTS `PowerPlantRoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `PowerPlantRoles` (
  `serverId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`roleId`),
  CONSTRAINT `PowerPlantRolesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Premium`
--

DROP TABLE IF EXISTS `Premium`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Premium` (
  `userId` bigint unsigned NOT NULL,
  `slot` tinyint unsigned NOT NULL DEFAULT '0',
  `serverId` bigint unsigned NOT NULL DEFAULT '0',
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`,`slot`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Reminders`
--

DROP TABLE IF EXISTS `Reminders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Reminders` (
  `id` bigint unsigned NOT NULL DEFAULT '0',
  `serverId` bigint unsigned NOT NULL,
  `sourceChannelId` bigint unsigned NOT NULL DEFAULT '0',
  `channelId` bigint unsigned NOT NULL,
  `messageId` bigint unsigned NOT NULL DEFAULT '0',
  `time` timestamp NOT NULL,
  `message` varchar(2048) NOT NULL,
  `interval` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `RemindersServerBase` (`serverId`),
  CONSTRAINT `RemindersServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SPBlock`
--

DROP TABLE IF EXISTS `SPBlock`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPBlock` (
  `serverId` bigint unsigned NOT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '0',
  `action` enum('DELETE_MESSAGE','KICK_USER','BAN_USER','') NOT NULL DEFAULT 'DELETE_MESSAGE',
  `blockURLName` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `SPBlockServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SPBlockIgnoredChannels`
--

DROP TABLE IF EXISTS `SPBlockIgnoredChannels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPBlockIgnoredChannels` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`channelId`),
  CONSTRAINT `SPBlockIgnoredChannelsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SPBlockIgnoredUsers`
--

DROP TABLE IF EXISTS `SPBlockIgnoredUsers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPBlockIgnoredUsers` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`userId`),
  KEY `SPBlockIgnoredUsersUserBase` (`userId`),
  CONSTRAINT `SPBlockIgnoredUsersServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SPBlockLogRecievers`
--

DROP TABLE IF EXISTS `SPBlockLogRecievers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SPBlockLogRecievers` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`userId`),
  KEY `SPBlockLogRecieversUserBase` (`userId`),
  CONSTRAINT `SPBlockLogRecieversServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServerInvites`
--

DROP TABLE IF EXISTS `ServerInvites`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ServerInvites` (
  `serverId` bigint unsigned NOT NULL,
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `usages` int unsigned NOT NULL,
  `maxAge` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`serverId`,`code`),
  CONSTRAINT `ServerInvitesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServerMute`
--

DROP TABLE IF EXISTS `ServerMute`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ServerMute` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `expires` timestamp NULL DEFAULT NULL,
  `newMethod` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`,`userId`),
  CONSTRAINT `ServerMuteServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `ServerWelcomeMessage`
--

DROP TABLE IF EXISTS `ServerWelcomeMessage`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ServerWelcomeMessage` (
  `serverId` bigint unsigned NOT NULL,
  `activated` tinyint(1) NOT NULL DEFAULT '0',
  `title` varchar(20) NOT NULL,
  `description` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `channel` bigint unsigned NOT NULL,
  `goodbye` tinyint(1) NOT NULL DEFAULT '0',
  `goodbyeText` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `goodbyeChannel` bigint unsigned NOT NULL,
  `dm` tinyint(1) NOT NULL DEFAULT '0',
  `dmText` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `ServerWelcomeMessageServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SlashPermissions`
--

DROP TABLE IF EXISTS `SlashPermissions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SlashPermissions` (
  `serverId` bigint unsigned NOT NULL,
  `command` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  `objectId` bigint unsigned NOT NULL,
  `objectType` tinyint unsigned NOT NULL,
  `allowed` tinyint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`command`,`objectId`) USING BTREE,
  CONSTRAINT `SlashPermissionsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `StaticReactionMessages`
--

DROP TABLE IF EXISTS `StaticReactionMessages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StaticReactionMessages` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  `messageId` bigint unsigned NOT NULL,
  `command` varchar(50) COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`messageId`),
  KEY `StaticReactionMessagesServerBase` (`serverId`),
  CONSTRAINT `StaticReactionMessagesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `StatsCommandUsages`
--

DROP TABLE IF EXISTS `StatsCommandUsages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StatsCommandUsages` (
  `date` date NOT NULL,
  `count` int unsigned NOT NULL,
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `StatsCommandUsagesExt`
--

DROP TABLE IF EXISTS `StatsCommandUsagesExt`;
/*!50001 DROP VIEW IF EXISTS `StatsCommandUsagesExt`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `StatsCommandUsagesExt` AS SELECT 
 1 AS `date`,
 1 AS `count`,
 1 AS `delta`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `StatsServerCount`
--

DROP TABLE IF EXISTS `StatsServerCount`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StatsServerCount` (
  `date` date NOT NULL,
  `count` int unsigned NOT NULL,
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `StatsServerCountExt`
--

DROP TABLE IF EXISTS `StatsServerCountExt`;
/*!50001 DROP VIEW IF EXISTS `StatsServerCountExt`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `StatsServerCountExt` AS SELECT 
 1 AS `date`,
 1 AS `count`,
 1 AS `delta`,
 1 AS `expectation`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `StatsServerCountMonthlyGrowth`
--

DROP TABLE IF EXISTS `StatsServerCountMonthlyGrowth`;
/*!50001 DROP VIEW IF EXISTS `StatsServerCountMonthlyGrowth`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `StatsServerCountMonthlyGrowth` AS SELECT 
 1 AS `mon`,
 1 AS `dailyGrowthInPercent`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `StatsServerCountWeeklyGrowth`
--

DROP TABLE IF EXISTS `StatsServerCountWeeklyGrowth`;
/*!50001 DROP VIEW IF EXISTS `StatsServerCountWeeklyGrowth`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `StatsServerCountWeeklyGrowth` AS SELECT 
 1 AS `week start`,
 1 AS `dailyGrowthInPercent`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `StatsUniqueUsers`
--

DROP TABLE IF EXISTS `StatsUniqueUsers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StatsUniqueUsers` (
  `date` date NOT NULL,
  `count` int unsigned NOT NULL,
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `StatsUpvotes`
--

DROP TABLE IF EXISTS `StatsUpvotes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StatsUpvotes` (
  `date` date NOT NULL,
  `totalUpvotes` int unsigned NOT NULL,
  `monthlyUpvotes` int unsigned NOT NULL,
  PRIMARY KEY (`date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `StatsUpvotesExt`
--

DROP TABLE IF EXISTS `StatsUpvotesExt`;
/*!50001 DROP VIEW IF EXISTS `StatsUpvotesExt`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `StatsUpvotesExt` AS SELECT 
 1 AS `date`,
 1 AS `totalUpvotes`,
 1 AS `monthlyUpvotes`,
 1 AS `delta`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `StickyRolesActions`
--

DROP TABLE IF EXISTS `StickyRolesActions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StickyRolesActions` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`userId`,`roleId`),
  CONSTRAINT `StickyRolesActionsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `StickyRolesRoles`
--

DROP TABLE IF EXISTS `StickyRolesRoles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `StickyRolesRoles` (
  `serverId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`roleId`),
  CONSTRAINT `StickyRolesRolesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Subs`
--

DROP TABLE IF EXISTS `Subs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Subs` (
  `command` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `memberId` bigint unsigned NOT NULL,
  `locale` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `errors` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`memberId`,`command`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SuggestionConfig`
--

DROP TABLE IF EXISTS `SuggestionConfig`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SuggestionConfig` (
  `serverId` bigint unsigned NOT NULL,
  `active` tinyint unsigned NOT NULL DEFAULT '0',
  `channelId` bigint unsigned DEFAULT '0',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `SuggestionConfigServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SuggestionMessages`
--

DROP TABLE IF EXISTS `SuggestionMessages`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SuggestionMessages` (
  `serverId` bigint unsigned NOT NULL,
  `messageId` bigint unsigned NOT NULL,
  `content` varchar(2048) NOT NULL,
  `author` varchar(100) NOT NULL,
  PRIMARY KEY (`serverId`,`messageId`),
  CONSTRAINT `SuggestionMessagesServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SurveyDates`
--

DROP TABLE IF EXISTS `SurveyDates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SurveyDates` (
  `surveyId` int unsigned NOT NULL,
  `start` date NOT NULL,
  PRIMARY KEY (`surveyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SurveyMajorityVotes`
--

DROP TABLE IF EXISTS `SurveyMajorityVotes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SurveyMajorityVotes` (
  `surveyId` int unsigned NOT NULL,
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `majorityVote` tinyint(1) NOT NULL,
  PRIMARY KEY (`surveyId`,`serverId`,`userId`),
  KEY `SurveyMajorityVoteServerBase` (`serverId`),
  KEY `SurveyMajorityVoteUserBase` (`userId`),
  CONSTRAINT `SurveyMajorityVoteServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `SurveyMajorityVoteSurveyBase` FOREIGN KEY (`surveyId`) REFERENCES `SurveyDates` (`surveyId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SurveyNotifications`
--

DROP TABLE IF EXISTS `SurveyNotifications`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SurveyNotifications` (
  `userId` bigint unsigned NOT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `SurveyVotes`
--

DROP TABLE IF EXISTS `SurveyVotes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `SurveyVotes` (
  `surveyId` int unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `personalVote` tinyint(1) NOT NULL,
  `locale` varchar(7) NOT NULL DEFAULT 'en_us',
  PRIMARY KEY (`surveyId`,`userId`),
  KEY `SurveyVotesUserBase` (`userId`),
  CONSTRAINT `SurveyVotesSurveyDatesBase` FOREIGN KEY (`surveyId`) REFERENCES `SurveyDates` (`surveyId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TempBans`
--

DROP TABLE IF EXISTS `TempBans`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TempBans` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `expires` timestamp NOT NULL,
  PRIMARY KEY (`serverId`,`userId`),
  CONSTRAINT `TempBanServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Ticket`
--

DROP TABLE IF EXISTS `Ticket`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Ticket` (
  `serverId` bigint unsigned NOT NULL,
  `counter` int unsigned NOT NULL DEFAULT '0',
  `channelId` bigint unsigned DEFAULT NULL,
  `memberCanClose` tinyint NOT NULL DEFAULT '1',
  `createMessage` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `assignToAll` tinyint unsigned NOT NULL DEFAULT '1',
  `protocol` tinyint unsigned NOT NULL DEFAULT '0',
  `ping` tinyint unsigned NOT NULL DEFAULT '1',
  `userMessages` tinyint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`),
  CONSTRAINT `TicketServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TicketOpenChannel`
--

DROP TABLE IF EXISTS `TicketOpenChannel`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TicketOpenChannel` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `messageChannelId` bigint unsigned NOT NULL,
  `messageMessageId` bigint unsigned NOT NULL,
  `assigned` tinyint unsigned NOT NULL DEFAULT '1',
  PRIMARY KEY (`serverId`,`channelId`),
  CONSTRAINT `TicketOpenChannelServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `TicketStaffRole`
--

DROP TABLE IF EXISTS `TicketStaffRole`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `TicketStaffRole` (
  `serverId` bigint unsigned NOT NULL,
  `roleId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`roleId`),
  CONSTRAINT `TicketStaffRoleServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Tracking`
--

DROP TABLE IF EXISTS `Tracking`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Tracking` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  `command` varchar(15) NOT NULL,
  `messageId` bigint unsigned DEFAULT NULL,
  `commandKey` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '',
  `time` timestamp NOT NULL,
  `arg` mediumtext,
  `webhookUrl` varchar(200) DEFAULT NULL,
  `userMessage` varchar(1024) DEFAULT NULL,
  `creationTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `minInterval` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`serverId`,`channelId`,`command`,`commandKey`) USING BTREE,
  CONSTRAINT `trackerServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Upvotes`
--

DROP TABLE IF EXISTS `Upvotes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Upvotes` (
  `userId` bigint unsigned NOT NULL,
  `lastDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `remindersSent` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `UserPrivateChannels`
--

DROP TABLE IF EXISTS `UserPrivateChannels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `UserPrivateChannels` (
  `userId` bigint unsigned NOT NULL,
  `privateChannelId` bigint unsigned NOT NULL,
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Version`
--

DROP TABLE IF EXISTS `Version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Version` (
  `version` char(8) NOT NULL,
  `date` timestamp NOT NULL,
  PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `Warnings`
--

DROP TABLE IF EXISTS `Warnings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `Warnings` (
  `serverId` bigint unsigned NOT NULL,
  `userId` bigint unsigned NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `requestorUserId` bigint unsigned NOT NULL,
  `reason` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`serverId`,`userId`,`time`),
  KEY `WarningsUserBase` (`userId`),
  KEY `WarningsRequestorUserBase` (`requestorUserId`),
  CONSTRAINT `WarningsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `WhiteListedChannels`
--

DROP TABLE IF EXISTS `WhiteListedChannels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `WhiteListedChannels` (
  `serverId` bigint unsigned NOT NULL,
  `channelId` bigint unsigned NOT NULL,
  PRIMARY KEY (`serverId`,`channelId`),
  CONSTRAINT `WhiteListedChannelsServerBase` FOREIGN KEY (`serverId`) REFERENCES `DServer` (`serverId`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping routines for database 'Lawliet'
--
/*!50003 DROP PROCEDURE IF EXISTS `RemoveUser` */;
/*!50003 SET @saved_cs_client      = @@character_set_client */ ;
/*!50003 SET @saved_cs_results     = @@character_set_results */ ;
/*!50003 SET @saved_col_connection = @@collation_connection */ ;
/*!50003 SET character_set_client  = utf8mb4 */ ;
/*!50003 SET character_set_results = utf8mb4 */ ;
/*!50003 SET collation_connection  = utf8mb4_0900_ai_ci */ ;
/*!50003 SET @saved_sql_mode       = @@sql_mode */ ;
/*!50003 SET sql_mode              = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION' */ ;
DELIMITER ;;
CREATE DEFINER=`Aninoss`@`%` PROCEDURE `RemoveUser`(
	IN `userIdRemove` BIGINT
)
    MODIFIES SQL DATA
BEGIN
	DELETE FROM AutoClaim WHERE userId = userIdRemove;
	DELETE FROM BannedWordsIgnoredUsers WHERE userId = userIdRemove;
	DELETE FROM BannedWordsLogRecievers WHERE userId = userIdRemove;
	DELETE FROM Donators WHERE userId = userIdRemove;
	DELETE FROM SPBlockIgnoredUsers WHERE userId = userIdRemove;
	DELETE FROM SPBlockLogRecievers WHERE userId = userIdRemove;
	DELETE FROM SurveyMajorityVotes WHERE userId = userIdRemove;
	DELETE FROM SurveyVotes WHERE userId = userIdRemove;
	DELETE FROM Upvotes WHERE userId = userIdRemove;
	DELETE FROM OsuAccounts WHERE userId = userIdRemove;
END ;;
DELIMITER ;
/*!50003 SET sql_mode              = @saved_sql_mode */ ;
/*!50003 SET character_set_client  = @saved_cs_client */ ;
/*!50003 SET character_set_results = @saved_cs_results */ ;
/*!50003 SET collation_connection  = @saved_col_connection */ ;

--
-- Current Database: `Lawliet`
--

USE `Lawliet`;

--
-- Final view structure for view `StatsCommandUsagesExt`
--

/*!50001 DROP VIEW IF EXISTS `StatsCommandUsagesExt`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`Aninoss`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `StatsCommandUsagesExt` AS select `a`.`date` AS `date`,`a`.`count` AS `count`,(`a`.`count` - `b`.`count`) AS `delta` from (`StatsCommandUsages` `a` join `StatsCommandUsages` `b` on((`a`.`date` = (`b`.`date` + interval 1 day)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `StatsServerCountExt`
--

/*!50001 DROP VIEW IF EXISTS `StatsServerCountExt`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`Aninoss`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `StatsServerCountExt` AS select `a`.`date` AS `date`,`a`.`count` AS `count`,(`a`.`count` - least(`b`.`count`,`a`.`count`)) AS `delta`,round((`a`.`count` * 0.009),0) AS `expectation` from (`StatsServerCount` `a` join `StatsServerCount` `b` on((`a`.`date` = (`b`.`date` + interval 1 day)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `StatsServerCountMonthlyGrowth`
--

/*!50001 DROP VIEW IF EXISTS `StatsServerCountMonthlyGrowth`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`Aninoss`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `StatsServerCountMonthlyGrowth` AS select date_format(`a`.`date`,'%Y-%m') AS `mon`,((sum(`a`.`delta`) / sum(`a`.`count`)) * 100) AS `dailyGrowthInPercent` from `StatsServerCountExt` `a` group by `mon` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `StatsServerCountWeeklyGrowth`
--

/*!50001 DROP VIEW IF EXISTS `StatsServerCountWeeklyGrowth`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`Aninoss`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `StatsServerCountWeeklyGrowth` AS select (`a`.`date` - interval (dayofweek(`a`.`date`) - 1) day) AS `week start`,((sum(`a`.`delta`) / sum(`a`.`count`)) * 100) AS `dailyGrowthInPercent` from `StatsServerCountExt` `a` group by `week start` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `StatsUpvotesExt`
--

/*!50001 DROP VIEW IF EXISTS `StatsUpvotesExt`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`Aninoss`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `StatsUpvotesExt` AS select `a`.`date` AS `date`,`a`.`totalUpvotes` AS `totalUpvotes`,`a`.`monthlyUpvotes` AS `monthlyUpvotes`,(`a`.`totalUpvotes` - least(`b`.`totalUpvotes`,`a`.`totalUpvotes`)) AS `delta` from (`StatsUpvotes` `a` join `StatsUpvotes` `b` on((`a`.`date` = (`b`.`date` + interval 1 day)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2022-11-12  9:54:39
