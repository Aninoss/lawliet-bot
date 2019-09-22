package DiscordListener;
import Commands.ServerManagement.WelcomeCommand;
import General.Tools;
import General.WelcomeMessageSetting;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMemberLeaveListener {
    public ServerMemberLeaveListener(){}

    public void onLeave(ServerMemberLeaveEvent event) {
        if (event.getUser().isYourself()) return;
        Server server = event.getServer();
        Locale locale = null;
        try {
            locale = DBServer.getServerLocale(server);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Verabschiedungen
        try {
            WelcomeMessageSetting welcomeMessageSetting = DBServer.getWelcomeMessageSettingFromServer(locale, server);
            if (welcomeMessageSetting != null && welcomeMessageSetting.isGoodbye()) {
                ServerTextChannel channel = welcomeMessageSetting.getFarewellChannel();
                if (channel.canYouWrite()) {
                    channel.sendMessage(
                            WelcomeCommand.replaceVariables(welcomeMessageSetting.getGoodbyeText(),
                                    server.getName(),
                                    "**" + event.getUser().getDiscriminatedName() + "**",
                                    Tools.numToString(locale, server.getMembers().size()))).get();
                }
            }
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser(), false);
        } catch (IOException | ExecutionException | SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}