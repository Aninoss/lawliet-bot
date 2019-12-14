package General;

import Constants.Settings;
import General.Internet.Internet;
import General.Internet.InternetResponse;
import ServerStuff.BotsOnDiscord;
import ServerStuff.CommunicationServer.CommunicationServer;
import ServerStuff.DiscordBotsAPI.DiscordbotsAPI;
import DiscordListener.*;
import ServerStuff.Donations.DonationServer;
import General.BotResources.ResourceManager;
import MySQL.*;
import ServerStuff.WebCommunicationServer.WebComServer;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioFormat;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.util.logging.ExceptionLogger;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class Connector {

    public static void main(String[] args) {
        try {
            //Redirect error outputs to a file
            if (!Bot.isDebug() && !Settings.TEST_MODE) {
                String fileName = new SimpleDateFormat("yyyy-MM-dd HH_mm_ss").format(new Date());
                File file = new File("data/error_log/" + fileName + "_err.log");
                FileOutputStream fos = new FileOutputStream(file);
                PrintStream ps = new PrintStream(fos);
                System.setErr(ps);
            }

            new CommunicationServer(35555); //Start Communication Server

            if (Settings.TEST_MODE) System.out.println("ATTENTION: The bot is running in test mode!");

            Console.getInstance().start(); //Starts Console Listener

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/impact.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Medium.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/Oswald-Regular.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/l_10646.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/seguisym.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/MS-UIGothic.ttf")));
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, new File("recourses/NotoEmoji.ttf")));
            DBMain.getInstance().connect();
            if (!Settings.TEST_MODE && !Bot.isDebug()) initializeUpdate();
            DiscordbotsAPI.getInstance().startWebhook();

            Arrays.stream(new File("temp").listFiles()).forEach(file -> file.delete()); //Cleans all temp files

            connect();
        } catch (SQLException | IOException | FontFormatException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private static Font getFont() {
        Graphics g = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB).getGraphics();
        Font font = new Font(g.getFont().toString(), 0, 12);
        g.dispose();

        return font;
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

    private static void connect() throws IOException {
        System.out.println("Bot is logging in...");


        DiscordApiBuilder apiBuilder = new DiscordApiBuilder()
            .setToken(SecretManager.getString((Bot.isDebug() && !Settings.TEST_MODE) ? "bot.token.debugger" : "bot.token"))
            .setRecommendedTotalShards().join();

        int totalShards = apiBuilder.getTotalShards();
        DiscordApiCollection.getInstance().init(totalShards);

        apiBuilder.loginAllShards()
            .forEach(shardFuture -> shardFuture
                    .thenAccept(Connector::onApiJoin)
                    .exceptionally(ExceptionLogger.get())
            );
    }

    public static void onApiJoin(DiscordApi api) {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();
        apiCollection.insertApi(api);
        api.setMessageCacheSize(10, 60 * 10);

        try {
            api.updateStatus(UserStatus.DO_NOT_DISTURB);
            api.updateActivity("Please wait, bot is booting up...");

            FisheryCache.getInstance(api.getCurrentShard()).startVCCollector(api);
            if (apiCollection.apiHasHomeServer(api)) {
                new WebComServer(15744);
                ResourceManager.setUp(apiCollection.getHomeServer());
            }
            if (apiCollection.allShardsConnected()) {
                new DonationServer(27440);
                DBMain.synchronizeAll();
                updateActivity();
            }

            System.out.printf("Shard %d has been successfully booten up!\n", api.getCurrentShard());

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
                    new VoiceChannelMemberJoinListener().onJoin(event);
                });
                addUncaughtException(t);
                t.setName("vc_member_join");
                t.start();
            });
            api.addServerVoiceChannelMemberLeaveListener(event -> {
                Thread t = new Thread(() -> new VoiceChannelMemberLeaveListener().onLeave(event));
                addUncaughtException(t);
                t.setName("vc_member_leave");
                t.start();
            });
            api.addServerMemberJoinListener(event -> {
                Thread t = new Thread(() -> {
                    new ServerMemberJoinListener().onJoin(event);
                });
                addUncaughtException(t);
                t.setName("member_join");
                t.start();
            });
            api.addServerMemberLeaveListener(event -> {
                Thread t = new Thread(() -> {
                    new ServerMemberLeaveListener().onLeave(event);
                });
                addUncaughtException(t);
                t.setName("member_leave");
                t.start();
            });
            api.addServerChannelDeleteListener(event -> {
                Thread t = new Thread(() -> {
                    new ServerChannelDeleteListener().onDelete(event);
                });
                addUncaughtException(t);
                t.setName("channel_delete");
                t.start();
            });
            api.addServerJoinListener(event -> {
                Thread t = new Thread(() -> {
                    new ServerJoinListener().onServerJoin(event);
                });
                addUncaughtException(t);
                t.setName("server_join");
                t.start();
            });
            api.addServerLeaveListener(event -> {
                Thread t = new Thread(() -> {
                    new ServerLeaveListener().onServerLeave(event);
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

            if (apiCollection.allShardsConnected() && !Bot.isDebug()) {
                Thread t = new Thread(Clock::tick);
                t.setPriority(2);
                addUncaughtException(t);
                t.setName("clock");
                t.start();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(0);
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
                calendar.get(Calendar.MINUTE) < 15;

        if (!isRestartPending) {
            if (DBMain.getInstance().checkConnection()) {
                api.updateStatus(UserStatus.ONLINE);
                api.updateActivity(ActivityType.WATCHING, "L.help | " + Tools.numToString(serverNumber) + " Servers | lawlietbot.xyz");
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
            System.err.println(t1.toString() + " has thrown an exception: " + e.getMessage());
            e.printStackTrace();
        });
    }

}