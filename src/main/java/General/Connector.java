package General;

import ServerStuff.CommunicationServer.CommunicationServer;
import ServerStuff.DiscordBotsAPI.DiscordbotsAPI;
import DiscordListener.*;
import ServerStuff.Donations.DonationServer;
import GUIPackage.GUI;
import General.BotResources.ResourceManager;
import MySQL.*;
//import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.entity.server.Server;
import java.awt.*;
import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;

public class Connector {
    public static void main(String[] args) {
        //Start GUI
        boolean withGUI = !Bot.isDebug();
        if (args.length > 0 && args[0].equals("nogui")) withGUI = false;
        if (withGUI) GUI.getInstance().start();

        //Start Communication Server
        CommunicationServer communicationServer = new CommunicationServer(35555);

        //Start WebCom Server
        //new WebComServer(15744);

        if (Bot.TEST_MODE) System.out.println("ATTENTION: The bot is running in test mode!");

        //Starts Console Listener
        new Thread(Console::manageConsole).start();
        addUncaughtException(Thread.currentThread());

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        try {
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/impact.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Medium.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/yumindb.ttf")));
            DBMain.getInstance().connect();
            if (!Bot.TEST_MODE && !Bot.isDebug()) initializeUpdate();
            DiscordbotsAPI.getInstance().startWebhook();
            connect(communicationServer);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private static void initializeUpdate() {
        try {
            String currentVersionDB = DBBot.getCurrentVersions();
            if (!Tools.getCurrentVersion().equals(currentVersionDB)) {
                DBBot.insertVersion(Tools.getCurrentVersion(), Instant.now());
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private static void connect(CommunicationServer communicationServer) {
        System.out.println("Bot is logging in...");

        DiscordApi api = null;
        try {

            api = new DiscordApiBuilder().setToken(SecretManager.getString((Bot.isDebug() && !Bot.TEST_MODE) ? "bot.token.debugger" : "bot.token")).login().join();

            new DonationServer(api, 27440);
            api.setMessageCacheSize(10, 60 * 5);
            communicationServer.setApi(api);
            FisheryCache.getInstance().startVCCollector(api);

            if (!Bot.TEST_MODE) {
                try {
                    api.updateStatus(UserStatus.DO_NOT_DISTURB);
                    api.updateActivity("Please wait, bot is booting up...");

                    System.out.println("Synchronizes Data...");
                    ResourceManager.setUp(Shortcuts.getHomeServer(api));
                    DBMain.synchronizeAll(api);
                    System.out.println("The bot has been successfully booten up!");
                    GUI.getInstance().setApi(api);

                    api.addMessageCreateListener(event -> {
                        Thread t = new Thread(() -> {
                            new MessageCreateListener().onMessageCreate(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addMessageEditListener(event -> {
                        Thread t = new Thread(() -> {
                            new MessageEditListener().onMessageEdit(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addMessageDeleteListener(event -> {
                        Thread t = new Thread(() -> {
                            new MessageDeleteListener().onMessageDelete(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addReactionAddListener(event -> {
                        Thread t = new Thread(() -> {
                            new ReactionAddListener().onReactionAdd(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addReactionRemoveListener(event -> {
                        Thread t = new Thread(() -> {
                            new ReactionRemoveListener().onReactionRemove(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerVoiceChannelMemberJoinListener(event -> {
                        Thread t = new Thread(() -> {
                            new VoiceChannelMemberJoinListener().onJoin(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerVoiceChannelMemberLeaveListener(event -> {
                        Thread t = new Thread(() -> new VoiceChannelMemberLeaveListener().onLeave(event));
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerMemberJoinListener(event -> {
                        Thread t = new Thread(() -> {
                            new ServerMemberJoinListener().onJoin(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerMemberLeaveListener(event -> {
                        Thread t = new Thread(() -> {
                            new ServerMemberLeaveListener().onLeave(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerChannelDeleteListener(event -> {
                        Thread t = new Thread(() -> {
                            new ServerChannelDeleteListener().onDelete(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerJoinListener(event -> {
                        Thread t = new Thread(() -> {
                            new ServerJoinListener().onServerJoin(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addServerLeaveListener(event -> {
                        Thread t = new Thread(() -> {
                            new ServerLeaveListener().onServerLeave(event);
                        });
                        addUncaughtException(t);
                        t.start();
                    });
                    api.addReconnectListener(event -> {
                        Thread t = new Thread(() -> onSessionResume(event.getApi()));
                        addUncaughtException(t);
                        t.run();
                    });

                    if (!Bot.isDebug()) {
                        final DiscordApi api2 = api;
                        Thread t = new Thread(() -> Clock.tick(api2));
                        addUncaughtException(t);
                        t.start();
                    }

                    updateActivity(api);
                } catch (Throwable e) {
                    e.printStackTrace();
                    api.disconnect();
                }
            } else {
                System.out.println("The bot has been successfully booten up!");

                //DOES NOTHING RIGHT NOW

                api.disconnect();
                System.out.println("Bot has been disconnected.");

                System.exit(0);
            }
        } catch (Throwable e) {
            e.printStackTrace();
            if (api != null) api.disconnect();
        }
    }

    private static void updateInactiveServerMembers(DiscordApi api) {
        for(Server server: api.getServers()) {
            ArrayList<Long> userIds = new ArrayList<>();
            for(User user: server.getMembers()) {
                userIds.add(user.getId());
            }

            try {
                for(RankingSlot rankingSlot: DBServer.getPowerPlantRankings(server)) {
                    if (!userIds.contains(rankingSlot.getUserId())) {
                        System.out.println(server.getName() + " | " + rankingSlot.getUserId());
                        DBUser.updateOnServerStatus(server, rankingSlot.getUserId(), false);
                    }
                }
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    public static void updateActivity(DiscordApi api) {
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity("L.help | " + api.getServers().size() + " Servers | v"+ Tools.getCurrentVersion());
    }

    private static void onSessionResume(DiscordApi api) {
        System.out.println("Verbindung wurde wiederhergestellt!");
        updateActivity(api);
    }

    private static void addUncaughtException(Thread t) {
        t.setUncaughtExceptionHandler((t1, e) -> System.err.println(t1.toString() + " has thrown an exception: " + e.getMessage()));
    }
}
