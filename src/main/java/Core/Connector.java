package Core;

import Constants.Settings;
import Core.Tools.StringTools;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.Tracker.DBTracker;
import MySQL.Modules.Version.DBVersion;
import MySQL.Modules.Version.VersionBean;
import MySQL.Modules.Version.VersionBeanSlot;
import ServerStuff.CommunicationServer;
import DiscordListener.*;
import Core.BotResources.ResourceManager;
import MySQL.*;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Connector {

    public static void main(String[] args) {
        try {
            boolean production = args.length >= 1 && args[0].equals("production");
            Bot.setDebug(production);

            Console.getInstance().start(); //Starts Console Listener

            //Check for faulty ports
            ArrayList<Integer> missingPort;
            if ((missingPort = checkPorts(35555, 15744)).size() > 0) {
                StringBuilder portsString = new StringBuilder();
                for(int port: missingPort) {
                    portsString.append(port).append(", ");
                }
                String portsStringFinal = portsString.toString();
                portsStringFinal = portsStringFinal.substring(0, portsStringFinal.length() - 2);

                System.err.printf("Error on port/s %s!\n", portsStringFinal);
                System.exit(1);
                return;
            }

            //Redirect error outputs to a file
            if (Bot.isProductionMode()) {
                String fileName = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(new Date());
                File file = new File("data/error_log/" + fileName + "_err.log");
                FileOutputStream fos = new FileOutputStream(file);
                PrintStream ps = new PrintStream(fos);
                System.setErr(ps);
            }

            new CommunicationServer(35555); //Start Communication Server

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/impact.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Medium.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/l_10646.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/seguisym.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/MS-UIGothic.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/NotoEmoji.ttf")));
            DBMain.getInstance().connect();

            Arrays.stream(new File("temp").listFiles()).forEach(File::delete); //Cleans all temp files

            if (Bot.isProductionMode()) initializeUpdate();
            connect();
        } catch (SQLException | IOException | FontFormatException e) {
            e.printStackTrace();
            ExceptionHandler.showErrorLog("Exception in main method");
            System.exit(-1);
        }
    }

    private static ArrayList<Integer> checkPorts(int... ports) {
        ArrayList<Integer> missingPorts = new ArrayList<>();

        for(int port: ports) {
            try {
                ServerSocket serverSocket = new ServerSocket(port);
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
                missingPorts.add(port);
            }
        }

        return missingPorts;
    }

    private static void initializeUpdate() {
        try {
            VersionBean versionBean = DBVersion.getInstance().getBean();

            String currentVersionDB = versionBean.getCurrentVersion().getVersion();
            if (!StringTools.getCurrentVersion().equals(currentVersionDB))
                versionBean.getSlots().add(new VersionBeanSlot(StringTools.getCurrentVersion(), Instant.now()));
        } catch (SQLException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static void connect() throws IOException {
        System.out.println("Bot is logging in...");

        DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
            .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
            .setRecommendedTotalShards().join();

        int totalShards = apiBuilder.getTotalShards();
        DiscordApiCollection.getInstance().init(totalShards);

        apiBuilder.loginAllShards()
            .forEach(shardFuture -> shardFuture
                    .thenAccept(api -> onApiJoin(api, true))
                    .exceptionally(ExceptionLogger.get())
            );
    }

    public static void reconnectApi(int shardId) {
        System.out.println("Reconnect shard " + shardId);

        try {
            DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
                    .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                    .setTotalShards(DiscordApiCollection.getInstance().size())
                    .setCurrentShard(shardId);

            DiscordApi api = apiBuilder.login().get();
            onApiJoin(api, false);
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
            ExceptionHandler.showErrorLog("Exception when reconnect shard " + shardId);
            System.exit(-1);
        }
    }

    public static void onApiJoin(DiscordApi api, boolean startup) {
        api.setAutomaticMessageCacheCleanupEnabled(true);
        api.setMessageCacheSize(10, Settings.TIME_OUT_TIME / 1000);

        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        apiCollection.insertApi(api);

        try {
            FisheryCache.getInstance(api.getCurrentShard()).startVCCollector(api);
            if (apiCollection.apiHasHomeServer(api) && startup) ResourceManager.setUp(apiCollection.getHomeServer());
            apiCollection.markReady(api);

            Thread st = new Thread(() -> DBAutoChannel.getInstance().synchronize(api));
            st.setName("synchro_shard_" + api.getCurrentShard());
            st.setPriority(1);
            st.start();

            ExceptionHandler.showInfoLog(String.format("Shard %d connection established!", api.getCurrentShard()));

            if (apiCollection.allShardsConnected()) {
                if (startup) {
                    updateActivity();
                    DBBotStats.fisheryCleanUp();
                    new WebComServer(15744);
                } else {
                    updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
                }
                ExceptionHandler.showInfoLog("All shards have been connected successfully!");

                Thread t = new Thread(Clock::tick);
                t.setPriority(1);
                addUncaughtException(t);
                t.setName("clock");
                t.start();

                if (Bot.isProductionMode()) DBTracker.getInstance().init();
            }

            api.addMessageCreateListener(event -> {
                Thread t = new Thread(() -> {
                    new MessageCreateListener().onMessageCreate(event);
                });
                addUncaughtException(t);
                t.setName("message_create");
                t.start();
            });
            api.addMessageEditListener(event -> {
                Thread t = new Thread(() -> {
                    new MessageEditListener().onMessageEdit(event);
                });
                addUncaughtException(t);
                t.setName("message_edit");
                t.start();
            });
            api.addMessageDeleteListener(event -> {
                Thread t = new Thread(() -> {
                    new MessageDeleteListener().onMessageDelete(event);
                });
                addUncaughtException(t);
                t.setName("message_delete");
                t.start();
            });
            api.addReactionAddListener(event -> {
                Thread t = new Thread(() -> {
                    new ReactionAddListener().onReactionAdd(event);
                });
                addUncaughtException(t);
                t.setName("reaction_add");
                t.start();
            });
            api.addReactionRemoveListener(event -> {
                Thread t = new Thread(() -> {
                    new ReactionRemoveListener().onReactionRemove(event);
                });
                addUncaughtException(t);
                t.setName("reaction_remove");
                t.start();
            });
            api.addServerVoiceChannelMemberJoinListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new VoiceChannelMemberJoinListener().onJoin(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("vc_member_join");
                t.start();
            });
            api.addServerVoiceChannelMemberLeaveListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new VoiceChannelMemberLeaveListener().onLeave(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("vc_member_leave");
                t.start();
            });
            api.addServerMemberJoinListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new ServerMemberJoinListener().onJoin(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("member_join");
                t.start();
            });
            api.addServerMemberLeaveListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new ServerMemberLeaveListener().onLeave(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("member_leave");
                t.start();
            });
            api.addServerChannelDeleteListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new ServerChannelDeleteListener().onDelete(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("channel_delete");
                t.start();
            });
            api.addServerJoinListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new ServerJoinListener().onServerJoin(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("server_join");
                t.start();
            });
            api.addServerLeaveListener(event -> {
                Thread t = new Thread(() -> {
                    try {
                        new ServerLeaveListener().onServerLeave(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                addUncaughtException(t);
                t.setName("server_leave");
                t.start();
            });
            api.addServerVoiceChannelChangeUserLimitListener(event -> {
                Thread t = new Thread(() -> {
                    new VoiceChannelChangeUserLimitListener().onVoiceChannelChangeUserLimit(event);
                });
                addUncaughtException(t);
                t.setName("server_change_userlimit");
                t.start();
            });
            api.addReconnectListener(event -> {
                Thread t = new Thread(() -> onSessionResume(event.getApi()));
                addUncaughtException(t);
                t.start();
            });
        } catch (Throwable e) {
            e.printStackTrace();
            ExceptionHandler.showErrorLog("Exception in connection method of shard " + api.getCurrentShard());
            System.exit(-1);
        }
    }

    public static void updateActivity() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        int serverTotalSize = apiCollection.getServerTotalSize();
        for(DiscordApi api: apiCollection.getApis()) {
            updateActivity(api, serverTotalSize);
        }
    }

    public static void updateActivity(DiscordApi api, int serverNumber) {
        Calendar calendar = Calendar.getInstance();
        boolean isRestartPending = calendar.get(Calendar.HOUR_OF_DAY) == 5 &&
                calendar.get(Calendar.MINUTE) < 15 &&
                Bot.hasUpdate();

        if (!isRestartPending) {
            if (DBMain.getInstance().checkConnection()) {
                api.updateStatus(UserStatus.ONLINE);
                api.updateActivity(ActivityType.WATCHING, "L.help | " + StringTools.numToString(serverNumber) + " | www.lawlietbot.xyz");
            } else {
                api.updateStatus(UserStatus.DO_NOT_DISTURB);
                api.updateActivity(ActivityType.WATCHING, "ERROR - DATABASE DOWN");
            }
        } else {
            api.updateStatus(UserStatus.DO_NOT_DISTURB);
            api.updateActivity(ActivityType.WATCHING, "BOT RESTARTS SOON");
        }
    }

    private static void onSessionResume(DiscordApi api) {
        System.out.println("Connection has been reestablished!");
        updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
    }

    private static void addUncaughtException(Thread t) {
        t.setUncaughtExceptionHandler((t1, e) -> {
            ExceptionHandler.showErrorLog(t1.toString() + " has thrown an exception: " + e.getMessage());
            e.printStackTrace();
        });
    }

}