package core;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.github.napstr.logback.DiscordAppender;
import com.jockie.jda.memory.MemoryOptimizations;
import core.emoji.EmojiTable;
import core.utils.BotUtil;
import mysql.MySQLManager;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionData;
import mysql.modules.version.VersionSlot;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncManager;

public class Main {

    public static void main(String[] args) {
        try { //TODO: remove?
            TextManager.getString(new Locale("en_US"), "casino", "casino_retry");
        } catch (Throwable e) {
            MainLogger.get().error("Error", e);
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            System.exit(1);
        }

        installMemoryOptimizations();
        try {
            registerErrorWebhook();
            Program.init();
            createTempDir();

            Console.start();
            FontManager.init();
            MySQLManager.connect();
            EmojiTable.load();
            if (Program.publicVersion()) {
                initializeUpdate();
            }

            if (Program.productionMode() || SystemUtils.IS_OS_WINDOWS) {
                MainLogger.get().info("Waiting for sync server");
                SyncManager.connect();
            }

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

    private static void registerErrorWebhook() {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        AsyncAppender discordAsync = (AsyncAppender) lc.getLogger(Logger.ROOT_LOGGER_NAME).getAppender("ASYNC_DISCORD");
        DiscordAppender discordAppender = (DiscordAppender) discordAsync.getAppender("DISCORD");
        discordAppender.setWebhookUri(System.getenv("ERROR_WEBHOOK"));
    }

    private static void installMemoryOptimizations() {
        try {
            MemoryOptimizations.installOptimizations();
        } catch (Throwable e) {
            MainLogger.get().error("Unable to install byte-buddy", e);
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
