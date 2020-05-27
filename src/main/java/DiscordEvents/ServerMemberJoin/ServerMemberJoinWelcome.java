package DiscordEvents.ServerMemberJoin;

import Commands.ManagementCategory.WelcomeCommand;
import Constants.Permission;
import Core.PermissionCheckRuntime;
import Core.Utils.StringUtil;
import DiscordEvents.DiscordEventAnnotation;
import DiscordEvents.EventTypeAbstracts.ServerMemberJoinAbstract;
import Modules.ImageCreator;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.WelcomeMessage.DBWelcomeMessage;
import MySQL.Modules.WelcomeMessage.WelcomeMessageBean;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@DiscordEventAnnotation
public class ServerMemberJoinWelcome extends ServerMemberJoinAbstract {

    final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinWelcome.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
        if (welcomeMessageBean.isWelcomeActive()) {
            welcomeMessageBean.getWelcomeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
                    InputStream image = ImageCreator.createImageWelcome(event.getUser(), server, welcomeMessageBean.getWelcomeTitle());
                    User user = event.getUser();
                    try {
                        if (image != null) {
                            channel.sendMessage(
                                    StringUtil.defuseMassPing(
                                            WelcomeCommand.replaceVariables(
                                                    welcomeMessageBean.getWelcomeText(),
                                                    server.getName(),
                                                    user.getMentionTag(),
                                                    user.getName(),
                                                    user.getDiscriminatedName(),
                                                    StringUtil.numToString(locale, server.getMemberCount())
                                            )
                                    ),
                                    image,
                                    "welcome.png"
                            ).get();
                        } else {
                            channel.sendMessage(
                                    StringUtil.defuseMassPing(
                                            WelcomeCommand.replaceVariables(
                                                    welcomeMessageBean.getWelcomeText(),
                                                    server.getName(),
                                                    user.getMentionTag(),
                                                    user.getName(),
                                                    user.getDiscriminatedName(),
                                                    StringUtil.numToString(locale, server.getMemberCount())
                                            )
                                    )
                            ).get();
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("Exception", e);
                    }
                }
            });
        }

        return true;
    }

}
