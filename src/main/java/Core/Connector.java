package Core;

import Constants.Settings;
import Core.Tools.StringTools;
import MySQL.Modules.AutoChannel.DBAutoChannel;
import MySQL.Modules.FisheryUsers.DBFishery;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Connector {

    final static Logger LOGGER = LoggerFactory.getLogger(Connector.class);

    public static void main(String[] args) {
        boolean production = args.length >= 1 && args[0].equals("production");
        Bot.setDebug(production);

        Console.getInstance().start(); //Starts Console Listener

        //Check for faulty ports
        ArrayList<Integer> missingPort;
        if ((missingPort = checkPorts(35555, 15744)).size() > 0) {
            StringBuilder portsString = new StringBuilder();
            for (int port : missingPort) {
                portsString.append(port).append(", ");
            }
            String portsStringFinal = portsString.toString();
            portsStringFinal = portsStringFinal.substring(0, portsStringFinal.length() - 2);

            System.err.printf("Error on port/s %s!\n", portsStringFinal);
            System.exit(1);
            return;
        }

        try {
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
            LOGGER.error("Exception in main method", e);
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
            LOGGER.error("Could not insert new update", e);
            System.exit(-1);
        }
    }

    private static void connect() throws IOException {
        LOGGER.info("Bot is logging in...");

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
        LOGGER.info("Shard {} is getting reconnected...", shardId);

        try {
            DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
                    .setToken(SecretManager.getString(Bot.isProductionMode() ? "bot.token" : "bot.token.debugger"))
                    .setTotalShards(DiscordApiCollection.getInstance().size())
                    .setCurrentShard(shardId);

            DiscordApi api = apiBuilder.login().get();
            onApiJoin(api, false);
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.error("Exception when reconnecting shard {}", shardId, e);
            System.exit(-1);
        }
    }

    public static void onApiJoin(DiscordApi api, boolean startup) {
        api.setAutomaticMessageCacheCleanupEnabled(true);
        api.setMessageCacheSize(30, 30 * 60);

        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        apiCollection.insertApi(api);

        try {
            if (apiCollection.apiHasHomeServer(api) && startup) ResourceManager.setUp(apiCollection.getHomeServer());
            apiCollection.markReady(api);

            Thread st = new CustomThread(() -> DBAutoChannel.getInstance().synchronize(api),
                    "autochannel_synchro_shard_" + api.getCurrentShard(),
                    1
            );
            st.start();

            LOGGER.info("Shard {} connection established", api.getCurrentShard());

            if (apiCollection.allShardsConnected()) {
                if (startup) {
                    updateActivity();
                    DBFishery.getInstance().cleanUp();
                    new WebComServer(15744);

                    Thread vcObserver = new CustomThread(() -> DBFishery.getInstance().startVCObserver(), "vc_observer", 1);
                    vcObserver.start();

                    LOGGER.info("All shards connected successfully");

                    Thread t = new CustomThread(Clock::getInstance, "clock", 1);
                    t.start();

                    if (Bot.isProductionMode()) DBTracker.getInstance().init();
                } else {
                    updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
                }
            }

            api.addMessageCreateListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new MessageCreateListener().onMessageCreate(event);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                }, "message_create");
                t.start();
            });
            api.addMessageEditListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new MessageEditListener().onMessageEdit(event);
                    } catch (InterruptedException e) {
                        LOGGER.error("Interrupted", e);
                    }
                }, "message_edit");
                t.start();
            });
            api.addMessageDeleteListener(event -> {
                Thread t = new CustomThread(() -> {
                    new MessageDeleteListener().onMessageDelete(event);
                }, "message_delete");
                t.start();
            });
            api.addReactionAddListener(event -> {
                Thread t = new CustomThread(() -> {
                    new ReactionAddListener().onReactionAdd(event);
                }, "reaction_add");
                t.start();
            });
            api.addReactionRemoveListener(event -> {
                Thread t = new CustomThread(() -> {
                    new ReactionRemoveListener().onReactionRemove(event);
                }, "reaction_remove");
                t.start();
            });
            api.addServerVoiceChannelMemberJoinListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new VoiceChannelMemberJoinListener().onJoin(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "vc_member_join");
                t.start();
            });
            api.addServerVoiceChannelMemberLeaveListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new VoiceChannelMemberLeaveListener().onLeave(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "vc_member_leave");
                t.start();
            });
            api.addServerMemberJoinListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new ServerMemberJoinListener().onJoin(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "member_join");
                t.start();
            });
            api.addServerMemberLeaveListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new ServerMemberLeaveListener().onLeave(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "member_leave");
                t.start();
            });
            api.addServerChannelDeleteListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new ServerChannelDeleteListener().onDelete(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "channel_delete");
                t.start();
            });
            api.addServerJoinListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new ServerJoinListener().onServerJoin(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "server_join");
                t.start();
            });
            api.addServerLeaveListener(event -> {
                Thread t = new CustomThread(() -> {
                    try {
                        new ServerLeaveListener().onServerLeave(event);
                    } catch (Exception e) {
                        LOGGER.error("Exception", e);
                    }
                }, "server_leave");
                t.start();
            });
            api.addServerVoiceChannelChangeUserLimitListener(event -> {
                Thread t = new CustomThread(() -> {
                    new VoiceChannelChangeUserLimitListener().onVoiceChannelChangeUserLimit(event);
                }, "server_change_userlimit");
                t.start();
            });
            api.addReconnectListener(event -> {
                Thread t = new CustomThread(() -> onSessionResume(event.getApi()), "reconnect");
                t.start();
            });
        } catch (Throwable e) {
            LOGGER.error("Exception in connection method of shard {}", api.getCurrentShard(), e);
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
        boolean isRestartPending = calendar.get(Calendar.HOUR_OF_DAY) == Settings.UPDATE_HOUR &&
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
        LOGGER.debug("Connection has been reestablished!");
        updateActivity(api, DiscordApiCollection.getInstance().getServerTotalSize());
    }

}