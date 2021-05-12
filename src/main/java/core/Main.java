package core;

import java.time.Instant;
import core.emoji.EmojiTable;
import core.utils.BotUtil;
import mysql.DBMain;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionData;
import mysql.modules.version.VersionSlot;
import websockets.syncserver.SyncManager;

public class Main {

    public static void main(String[] args) {
        try {
            Program.init();
            createTempDir();

            Console.getInstance().start();
            FontContainer.getInstance().init();
            DBMain.getInstance().connect();
            EmojiTable.getInstance().load();
            if (Program.isPublicVersion()) {
                initializeUpdate();
            }

            MainLogger.get().info("Waiting for sync server");
            SyncManager.getInstance().start();

            if (!Program.isProductionMode()) {
                DiscordConnector.getInstance().connect(0, 0, 1);
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread(Program::onStop, "Shutdown Bot-Stop"));
            }
        } catch (Throwable e) {
            MainLogger.get().error("EXIT - Error on startup", e);
            System.exit(1);
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
            versionData.getSlots().add(new VersionSlot(BotUtil.getCurrentVersion(), Instant.now()));
        }
    }

}
