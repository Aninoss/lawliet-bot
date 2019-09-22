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
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.user.UserStatus;
import org.javacord.api.entity.server.Server;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Connector {
    public static void main(String[] args) throws IOException, FontFormatException {
        //Start GUI
        boolean withGUI = !Bot.isDebug();
        if (args.length > 0 && args[0].equals("nogui")) withGUI = false;
        if (withGUI) GUI.getInstance().start();

        CommunicationServer communicationServer = new CommunicationServer(35555); //Start Communication Server

        if (Bot.TEST_MODE) System.out.println("ATTENTION: The bot is running in test mode!");

        new Thread(Console::manageConsole).start(); //Starts Console Listener

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

                api.addMessageCreateListener(event ->
                    new Thread(() -> new MessageCreateListener().onMessageCreate(event)).start()
                );
                api.addMessageEditListener(event ->
                        new Thread(() -> new MessageEditListener().onMessageEdit(event)).start()
                );
                api.addMessageDeleteListener(event ->
                        new Thread(() -> new MessageDeleteListener().onMessageDelete(event)).start()
                );
                api.addReactionAddListener(event ->
                        new Thread(() -> new ReactionAddListener().onReactionAdd(event)).start()
                );
                api.addReactionRemoveListener(event ->
                        new Thread(() -> new ReactionRemoveListener().onReactionRemove(event)).start()
                );
                api.addServerVoiceChannelMemberJoinListener(event ->
                        new Thread(() -> new VoiceChannelMemberJoinListener().onJoin(event)).start()
                );
                api.addServerVoiceChannelMemberLeaveListener(event ->
                        new Thread(() -> new VoiceChannelMemberLeaveListener().onLeave(event)).start()
                );
                api.addServerMemberJoinListener(event ->
                        new Thread(() -> new ServerMemberJoinListener().onJoin(event)).start()
                );
                api.addServerMemberLeaveListener(event ->
                        new Thread(() -> new ServerMemberLeaveListener().onLeave(event)).start()
                );
                api.addServerChannelDeleteListener(event ->
                        new Thread(() -> new ServerChannelDeleteListener().onDelete(event)).start()
                );
                api.addServerJoinListener(event ->
                        new Thread(() -> new ServerJoinListener().onServerJoin(event)).start()
                );
                api.addServerLeaveListener(event ->
                        new Thread(() -> new ServerLeaveListener().onServerLeave(event)).start()
                );
                api.addReconnectListener(event ->
                        new Thread(() -> onSessionResume(event.getApi())).run()
                );

                if (!Bot.isDebug()) {
                    new Thread(() -> Clock.tick(api)).start();
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
}
