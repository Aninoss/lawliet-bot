package events.discordevents.servermemberjoin;

import commands.runnables.managementcategory.WelcomeCommand;
import constants.Permission;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.ServerMemberJoinAbstract;
import modules.ImageCreator;
import modules.Welcome;
import mysql.modules.server.DBServer;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.server.member.ServerMemberJoinEvent;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Locale;

@DiscordEvent(allowBots = true)
public class ServerMemberJoinWelcome extends ServerMemberJoinAbstract {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerMemberJoinWelcome.class);

    @Override
    public boolean onServerMemberJoin(ServerMemberJoinEvent event) throws Throwable {
        Server server = event.getServer();
        Locale locale = DBServer.getInstance().getBean(server.getId()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().getBean(server.getId());
        if (welcomeMessageBean.isDmActive()) {
            sendDmMessage(event, welcomeMessageBean, locale);
        }

        if (welcomeMessageBean.isWelcomeActive()) {
            welcomeMessageBean.getWelcomeChannel().ifPresent(channel -> {
                sendWelcomeMessage(event, welcomeMessageBean, channel, locale);
            });
        }

        return true;
    }

    private void sendDmMessage(ServerMemberJoinEvent event, WelcomeMessageBean welcomeMessageBean, Locale locale) {
        Server server = event.getServer();
        User user = event.getUser();
        String text = welcomeMessageBean.getDmText();

        if (text.length() > 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setAuthor(server.getName(), "", server.getIcon().map(icon -> icon.getUrl().toString()).orElse(""))
                    .setDescription(
                            Welcome.resolveVariables(
                                welcomeMessageBean.getDmText(),
                                StringUtil.escapeMarkdown(server.getName()),
                                user.getMentionTag(),
                                StringUtil.escapeMarkdown(user.getName()),
                                StringUtil.escapeMarkdown(user.getDiscriminatedName()),
                                StringUtil.numToString(server.getMemberCount())
                        )
                    );
            event.getUser().sendMessage(eb)
                    .exceptionally(ExceptionLogger.get());
        }
    }

    private void sendWelcomeMessage(ServerMemberJoinEvent event, WelcomeMessageBean welcomeMessageBean, ServerTextChannel channel, Locale locale) {
        Server server = channel.getServer();

        if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.READ_MESSAGES | Permission.SEND_MESSAGES | Permission.EMBED_LINKS | Permission.ATTACH_FILES)) {
            InputStream image = ImageCreator.createImageWelcome(event.getUser(), server, welcomeMessageBean.getWelcomeTitle());
            User user = event.getUser();
            if (image != null) {
                channel.sendMessage(
                        StringUtil.defuseMassPing(
                                Welcome.resolveVariables(
                                        welcomeMessageBean.getWelcomeText(),
                                        StringUtil.escapeMarkdown(server.getName()),
                                        user.getMentionTag(),
                                        StringUtil.escapeMarkdown(user.getName()),
                                        StringUtil.escapeMarkdown(user.getDiscriminatedName()),
                                        StringUtil.numToString(server.getMemberCount())
                                )
                        ),
                        image,
                        "welcome.png"
                ).exceptionally(ExceptionLogger.get());
            } else {
                channel.sendMessage(
                        StringUtil.defuseMassPing(
                                Welcome.resolveVariables(
                                        welcomeMessageBean.getWelcomeText(),
                                        StringUtil.escapeMarkdown(server.getName()),
                                        user.getMentionTag(),
                                        StringUtil.escapeMarkdown(user.getName()),
                                        StringUtil.escapeMarkdown(user.getDiscriminatedName()),
                                        StringUtil.numToString(server.getMemberCount())
                                )
                        )
                ).exceptionally(ExceptionLogger.get());
            }
        }
    }

}
