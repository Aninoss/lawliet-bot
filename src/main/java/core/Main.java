package core;

import core.cache.PatreonCache;
import core.emoji.EmojiTable;
import core.utils.BotUtil;
import events.sync.EventManager;
import mysql.MySQLManager;
import mysql.hibernate.HibernateManager;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionData;
import mysql.modules.version.VersionSlot;

import java.time.Instant;

public class Main {

    public static void main(String[] args) {
        try {
            Program.init();
            createTempDir();

            Console.start();
            FontManager.init();
            HibernateManager.connect();
            MySQLManager.connect();
            EmojiTable.load();
            if (Program.productionMode()) {
                PatreonCache.getInstance().fetch();
                HeartbeatReceiver.start();
            }
            if (Program.publicInstance()) {
                initializeUpdate();
            }

            EventManager.register();
            if (!Program.productionMode() || !Program.publicInstance()) {
                DiscordConnector.connect(0, 0, 1);
            }
            if (Program.productionMode()) {
                Runtime.getRuntime().addShutdownHook(new Thread(Program::onStop, "Shutdown Bot-Stop"));
            }
        } catch (Throwable e) {
            MainLogger.get().error("EXIT - Error on startup");
            e.printStackTrace();
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

}
