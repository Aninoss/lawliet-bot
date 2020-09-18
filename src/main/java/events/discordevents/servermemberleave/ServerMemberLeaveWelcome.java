package events.discordevents.servermemberleave;

import commands.runnables.managementcategory.WelcomeCommand;
import constants.Permission;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberLeaveAbstract;
import modules.Welcome;
import mysql.modules.server.DBServer;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordEvent(allowBots = true)
public class ServerMemberLeaveWelcome extends ServerMemberLeaveAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberLeaveWelcome.class);

    @Override
    public boolean onServerMemberLeave(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
        if (welcomeMessageBean.isGoodbyeActive()) {
            welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
                    User user = event.getUser();
                    try {
                        channel.sendMessage(
                                StringUtil.defuseMassPing(
                                        Welcome.resolveVariables(
                                                welcomeMessageBean.getGoodbyeText(),
                                                StringUtil.escapeMarkdown(server.getName()),
                                                user.getMentionTag(),
                                                StringUtil.escapeMarkdown(user.getName()),
                                                StringUtil.escapeMarkdown(user.getDiscriminatedName()),
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

        return true;
    }

}
