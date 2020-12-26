package core;

import core.utils.BotUtil;
import mysql.DBMain;
import mysql.modules.version.DBVersion;
import mysql.modules.version.VersionBean;
import mysql.modules.version.VersionBeanSlot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import websockets.syncserver.SyncManager;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Throwable {
        boolean production = args.length >= 1 && args[0].equals("production");
        int clusterId = args.length >= 2 ? Integer.parseInt(args[1]) : 0;
        Bot.init(production, clusterId);
        Runtime.getRuntime().addShutdownHook(new CustomThread(Bot::onStop, "shutdown_botstop"));

        Console.getInstance().start();
        FontContainer.getInstance().init();
        DBMain.getInstance().connect();
        if (Bot.isPublicVersion()) {
            cleanAllTempFiles();
            initializeUpdate();
        }

        LOGGER.info("Waiting for sync server");
        SyncManager.getInstance().start();
    }

    private static void cleanAllTempFiles() {
        File[] files = new File("temp").listFiles();
        if (files != null)
            Arrays.stream(files).forEach(file -> {
                if (!file.delete()) {
                    LOGGER.error("Temp file {} could not be removed!", file.getName());
                }
            });
    }

    private static void initializeUpdate() {
        VersionBean versionBean = DBVersion.getInstance().getBean();

        String currentVersionDB = versionBean.getCurrentVersion().getVersion();
        if (!BotUtil.getCurrentVersion().equals(currentVersionDB))
            versionBean.getSlots().add(new VersionBeanSlot(BotUtil.getCurrentVersion(), Instant.now()));
    }

}
