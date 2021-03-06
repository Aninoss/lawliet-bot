package events.discordevents.guildmemberremove;

import commands.runnables.utilitycategory.WelcomeCommand;
import constants.PermissionDeprecated;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.Welcome;
import mysql.modules.server.DBServer;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberLeaveEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.Locale;

@DiscordEvent(allowBots = true)
public class GuildMemberRemoveWelcome extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(ServerMemberLeaveEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().retrieve(server.getId()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(server.getId());
        if (welcomeMessageBean.isGoodbyeActive()) {
            welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, PermissionDeprecated.READ_MESSAGES | PermissionDeprecated.SEND_MESSAGES | PermissionDeprecated.EMBED_LINKS | PermissionDeprecated.ATTACH_FILES)) {
                    User user = event.getUser();
                    channel.sendMessage(
                            StringUtil.defuseMassPing(
                                    Welcome.resolveVariables(
                                            welcomeMessageBean.getGoodbyeText(),
                                            StringUtil.escapeMarkdown(server.getName()),
                                            user.getMentionTag(),
                                            StringUtil.escapeMarkdown(user.getName()),
                                            StringUtil.escapeMarkdown(user.getDiscriminatedName()),
                                            StringUtil.numToString(server.getMemberCount())
                                    )
                            )
                    ).exceptionally(ExceptionLogger.get());
                }
            });
        }

        return true;
    }

}
