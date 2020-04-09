package DiscordListener;
import Commands.ManagementCategory.MemberCountDisplayCommand;
import Commands.ManagementCategory.WelcomeCommand;
import Constants.Permission;
import Core.PermissionCheckRuntime;
import Core.Tools.StringTools;
import MySQL.DBUser;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.WelcomeMessage.DBWelcomeMessage;
import MySQL.Modules.WelcomeMessage.WelcomeMessageBean;
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
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        //Update on server status
        try {
            if (!event.getUser().isBot()) DBUser.updateOnServerStatus(server, event.getUser().getId(), false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        //Verabschiedungen
        try {
            WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
            if (welcomeMessageBean.isGoodbyeActive()) {
                welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
                        User user = event.getUser();
                        try {
                            channel.sendMessage(
                                    StringTools.defuseMassPing(
                                            WelcomeCommand.replaceVariables(
                                                    welcomeMessageBean.getGoodbyeText(),
                                                    server.getName(),
                                                    user.getMentionTag(),
                                                    user.getName(),
                                                    user.getDiscriminatedName(),
                                                    StringTools.numToString(locale, server.getMembers().size())
                                            )
                                    )
                            ).get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //Member Count Stats
        try {
            MemberCountDisplayCommand.manage(locale, server);
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
    }
}