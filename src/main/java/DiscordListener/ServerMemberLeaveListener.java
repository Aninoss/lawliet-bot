package DiscordListener;
import Commands.ManagementCategory.MemberCountDisplayCommand;
import Commands.ManagementCategory.WelcomeCommand;
import Constants.Locales;
import Constants.Permission;
import General.PermissionCheckRuntime;
import General.Tools;
import General.WelcomeMessageSetting;
import MySQL.DBServerOld;
import MySQL.DBUser;
import MySQL.Server.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;

import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMemberLeaveListener {

    public void onLeave(ServerMemberLeaveEvent event) throws Exception {
        if (event.getUser().isYourself()) return;
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getServerBean(event.getServer().getId()).getLocale();

        //Update on server status
        try {
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser().getId(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Verabschiedungen
        try {
            WelcomeMessageSetting welcomeMessageSetting = DBServerOld.getWelcomeMessageSettingFromServer(locale, server);
            if (welcomeMessageSetting != null && welcomeMessageSetting.isGoodbye()) {
                ServerTextChannel channel = welcomeMessageSetting.getFarewellChannel();
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, "welcome", channel, Permission.WRITE_IN_TEXT_CHANNEL | Permission.EMBED_LINKS_IN_TEXT_CHANNELS | Permission.ATTACH_FILES_TO_TEXT_CHANNEL)) {
                    User user = event.getUser();
                    channel.sendMessage(
                            Tools.defuseMassPing(WelcomeCommand.replaceVariables(welcomeMessageSetting.getGoodbyeText(),
                                    server.getName(),
                                    user.getMentionTag(),
                                    user.getName(),
                                    user.getDiscriminatedName(),
                                    Tools.numToString(locale, server.getMembers().size())))).get();
                }
            }
        } catch (ExecutionException | SQLException | InterruptedException e) {
            e.printStackTrace();
        }

        //Member Count Stats
        try {
            MemberCountDisplayCommand.manage(locale, server);
        } catch (SQLException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}