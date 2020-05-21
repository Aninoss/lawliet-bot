package DiscordListener.Obsolete;
import Commands.ManagementCategory.MemberCountDisplayCommand;
import Commands.ManagementCategory.WelcomeCommand;
import Constants.Permission;
import Core.PermissionCheckRuntime;
import Core.Utils.StringUtil;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.WelcomeMessage.DBWelcomeMessage;
import MySQL.Modules.WelcomeMessage.WelcomeMessageBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMemberLeaveListener {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeaveListener.class);

    public void onLeave(ServerMemberLeaveEvent event) throws Exception {
        if (event.getUser().isYourself()) return;
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        //Verabschiedungen
        try {
            WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
            if (welcomeMessageBean.isGoodbyeActive()) {
                welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
                        User user = event.getUser();
                        try {
                            channel.sendMessage(
                                    StringUtil.defuseMassPing(
                                            WelcomeCommand.replaceVariables(
                                                    welcomeMessageBean.getGoodbyeText(),
                                                    server.getName(),
                                                    user.getMentionTag(),
                                                    user.getName(),
                                                    user.getDiscriminatedName(),
                                                    StringUtil.numToString(locale, server.getMemberCount())
                                            )
                                    )
                            ).get();
                        } catch (InterruptedException | ExecutionException e) {
                            LOGGER.error("Could not send message", e);
                        }
                    }
                });
            }
        } catch (ExecutionException e) {
            LOGGER.error("Could not get welcome bean", e);
        }

        //Member Count Stats
        try {
            MemberCountDisplayCommand.manage(locale, server);
        } catch (ExecutionException e) {
            LOGGER.error("Exception", e);
        }
    }
}