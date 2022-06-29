package core;

import java.time.Instant;
import core.cache.PatreonCache;
import core.emoji.EmojiTable;
import core.utils.BotUtil;
import events.sync.EventManager;
import mysql.MySQLManager;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionData;
import mysql.modules.version.VersionSlot;

public class Main {

    public static void main(String[] args) {
        try {
            Program.init();
            createTempDir();

            Console.start();
            FontManager.init();
            MySQLManager.connect();
            EmojiTable.load();
            if (Program.productionMode()) {
                PatreonCache.getInstance().fetch();
            }
            if (Program.publicVersion()) {
                initializeUpdate();
            }

            EventManager.register();
            if (!Program.productionMode()) {
                DiscordConnector.connect(0, 0, 1);
            } else {
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

}
