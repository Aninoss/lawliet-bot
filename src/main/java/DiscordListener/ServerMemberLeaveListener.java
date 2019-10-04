package DiscordListener;
import Commands.ServerManagement.WelcomeCommand;
import Constants.Permission;
import General.PermissionCheckRuntime;
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

        //Update on server status
        try {
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Verabschiedungen
        try {
            WelcomeMessageSetting welcomeMessageSetting = DBServer.getWelcomeMessageSettingFromServer(locale, server);
            if (welcomeMessageSetting != null && welcomeMessageSetting.isGoodbye()) {
                ServerTextChannel channel = welcomeMessageSetting.getFarewellChannel();
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "welcome", channel, Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS | Permission.ATTACH_FILES_TO_TEXT_CHANNEL)) {
                    channel.sendMessage(
                            WelcomeCommand.replaceVariables(welcomeMessageSetting.getGoodbyeText(),
                                    server.getName(),
                                    "**" + event.getUser().getDiscriminatedName() + "**",
                                    Tools.numToString(locale, server.getMembers().size()))).get();
                }
            }
        } catch (IOException | ExecutionException | SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}