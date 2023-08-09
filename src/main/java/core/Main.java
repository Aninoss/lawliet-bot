package core;

import constants.Language;
import core.cache.PatreonCache;
import core.emoji.EmojiTable;
import core.utils.BotUtil;
import events.sync.EventManager;
import mysql.MySQLManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.FisheryEntity;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import mysql.modules.guild.GuildKickedData;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionData;
import mysql.modules.version.VersionSlot;

import java.time.Instant;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try {
            Program.init();
            createTempDir();

            Console.start();
            FontManager.init();
            HibernateManager.connect();
            MySQLManager.connect();
            transferSqlGuildData();
            EmojiTable.load();
            if (Program.productionMode()) {
                PatreonCache.getInstance().fetch();
            }
            if (Program.publicVersion()) {
                initializeUpdate();
            }

            EventManager.register();
            if (!Program.productionMode() || !Program.publicVersion()) {
                DiscordConnector.connect(0, 0, 1);
            }
            if (Program.productionMode()) {
                Runtime.getRuntime().addShutdownHook(new Thread(Program::onStop, "Shutdown Bot-Stop"));
            }
        } catch (Throwable e) {
            MainLogger.get().error("EXIT - Error on startup", e);
            System.exit(4);
        }
    }

    private static void createTempDir() {
        LocalFile tempDir = new LocalFile("temp");
        if (!tempDir.exists() && !tempDir.mkdir()) {
            throw new RuntimeException("Could not create temp dir");
        }
    }

    private static void initializeUpdate() {
        VersionData versionData = DBVersion.getInstance().retrieve();
        String currentVersionDB = versionData.getCurrentVersion().getVersion();
        if (!BotUtil.getCurrentVersion().equals(currentVersionDB)) {
            Program.setNewVersion();
            versionData.getSlots().add(new VersionSlot(BotUtil.getCurrentVersion(), Instant.now()));
        }
    }

    private static void transferSqlGuildData() {
        if (!Program.productionMode() || !Program.publicVersion()) {
            return;
        }

        MainLogger.get().info("Transferring MySQL data to MongoDB...");
        List<GuildKickedData> guildKickedDataList;
        int limit = 100;
        long guildIdOffset = 0;
        do {
            guildKickedDataList = DBGuild.getInstance().retrieveKickedData(guildIdOffset, limit);
            for (GuildKickedData guildKickedData : guildKickedDataList) {
                try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager()) {
                    GuildData guildData = DBGuild.getInstance().retrieve(guildKickedData.getGuildId());
                    FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guildKickedData.getGuildId());
                    GuildEntity guildEntity = entityManager.find(GuildEntity.class, String.valueOf(guildData.getGuildId()));
                    if (guildEntity == null) {
                        guildEntity = new GuildEntity(String.valueOf(guildData.getGuildId()));
                        guildEntity.setPrefix(guildData.getPrefix());
                        guildEntity.setLanguage(Language.from(guildData.getLocale()));
                        guildEntity.setRemoveAuthorMessage(guildData.isCommandAuthorMessageRemove());
                        guildEntity.setFishery(getFisheryEntity(guildData, fisheryGuildData));

                        entityManager.getTransaction().begin();
                        entityManager.persist(guildEntity);
                        entityManager.getTransaction().commit();
                    } else {
                        entityManager.getTransaction().begin();
                        guildEntity.setFishery(getFisheryEntity(guildData, fisheryGuildData));
                        entityManager.getTransaction().commit();
                    }
                }
            }
            if (!guildKickedDataList.isEmpty()) {
                guildIdOffset = guildKickedDataList.get(guildKickedDataList.size() - 1).getGuildId();
            }
        } while (guildKickedDataList.size() == limit);
    }

    private static FisheryEntity getFisheryEntity(GuildData guildData, FisheryGuildData fisheryGuildData) {
        FisheryEntity fisheryEntity = new FisheryEntity();
        fisheryEntity.setFisheryStatus(guildData.getFisheryStatus());
        fisheryEntity.setTreasureChests(guildData.isFisheryTreasureChests());
        fisheryEntity.setPowerUps(guildData.isFisheryPowerups());
        fisheryEntity.setFishReminders(guildData.isFisheryReminders());
        fisheryEntity.setCoinGiftLimit(guildData.hasFisheryCoinsGivenLimit());
        fisheryEntity.setExcludedChannelIds(fisheryGuildData.getIgnoredChannelIds());
        fisheryEntity.setRoleIds(fisheryGuildData.getRoleIds());
        fisheryEntity.setSingleRoles(guildData.isFisherySingleRoles());
        fisheryEntity.setRoleUpgradeChannelId(guildData.getFisheryAnnouncementChannelId().orElse(null));
        fisheryEntity.setRolePriceMin(guildData.getFisheryRoleMin());
        fisheryEntity.setRolePriceMax(guildData.getFisheryRoleMax());

        Integer voiceHoursLimit = guildData.getFisheryVcHoursCap().orElse(null);
        if (voiceHoursLimit == null) {
            voiceHoursLimit = 5;
        }
        if (voiceHoursLimit == 0) {
            voiceHoursLimit = null;
        }

        fisheryEntity.setVoiceHoursLimit(voiceHoursLimit);
        return fisheryEntity;
    }

}
