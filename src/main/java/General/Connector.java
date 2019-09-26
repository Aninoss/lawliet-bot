package General;

import ServerStuff.CommunicationServer.CommunicationServer;
import ServerStuff.DiscordBotsAPI.DiscordbotsAPI;
import DiscordListener.*;
import ServerStuff.Donations.DonationServer;
import GUIPackage.GUI;
import General.BotResources.ResourceManager;
import MySQL.*;
//import ServerStuff.WebCommunicationServer.WebComServer;
import ServerStuff.WebCommunicationServer.WebComServer;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.auditlog.AuditLogEntry;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageSet;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.entity.server.Server;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Connector {

    public static void main(String[] args) throws IOException, FontFormatException {
        //Redirect error outputs to a file
        String fileName = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(new Date());
        File file = new File("data/error_log/" + fileName + "_err.log");
        FileOutputStream fos = new FileOutputStream(file);
        PrintStream ps = new PrintStream(fos);
        System.setErr(ps);

        CommunicationServer communicationServer = new CommunicationServer(35555); //Start Communication Server

        if (Bot.TEST_MODE) System.out.println("ATTENTION: The bot is running in test mode!");

        Console.getInstance().start(); //Starts Console Listener

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/impact.ttf")));
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Medium.ttf")));
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Regular.ttf")));
        ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/l_10646.ttf")));
        DBMain.getInstance().connect();
        if (!Bot.TEST_MODE && !Bot.isDebug()) initializeUpdate();
        DiscordbotsAPI.getInstance().startWebhook();

        connect(communicationServer);
    }

    private static void initializeUpdate() {
        try {
            String currentVersionDB = DBBot.getCurrentVersions();
            if (!Tools.getCurrentVersion().equals(currentVersionDB)) {
                DBBot.insertVersion(Tools.getCurrentVersion(), Instant.now());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void connect(CommunicationServer communicationServer) throws IOException {
        System.out.println("Bot is logging in...");

        DiscordApi api = new DiscordApiBuilder().setToken(SecretManager.getString((Bot.isDebug() && !Bot.TEST_MODE) ? "bot.token.debugger" : "bot.token")).login().join();
        api.setMessageCacheSize(10, 60 * 10);

        if (!Bot.TEST_MODE) {
            try {
                api.updateStatus(UserStatus.DO_NOT_DISTURB);
                api.updateActivity("Please wait, bot is booting up...");

                System.out.println("Synchronizes Data...");

                new DonationServer(api, 27440);
                communicationServer.setApi(api);
                FisheryCache.getInstance().startVCCollector(api);
                new WebComServer(15744, api);
                ResourceManager.setUp(Shortcuts.getHomeServer(api));
                DBMain.synchronizeAll(api);

                System.out.println("The bot has been successfully booten up!");

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
                    Thread t = new Thread(() -> Clock.tick(api));
                    addUncaughtException(t);
                    t.start();
                }

                updateActivity(api);
            } catch (InterruptedException | ExecutionException | SQLException e) {
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
    }

    //Not used in the final version
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
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void updateActivity(DiscordApi api) {
        api.updateStatus(UserStatus.ONLINE);
        api.updateActivity("L.help | " + api.getServers().size() + " Servers | v"+ Tools.getCurrentVersion());
    }

    private static void onSessionResume(DiscordApi api) {
        System.out.println("Connection has been reestablished!");
        updateActivity(api);
    }

    private static void addUncaughtException(Thread t) {
        t.setUncaughtExceptionHandler((t1, e) -> System.err.println(t1.toString() + " has thrown an exception: " + e.getMessage()));
    }
}
