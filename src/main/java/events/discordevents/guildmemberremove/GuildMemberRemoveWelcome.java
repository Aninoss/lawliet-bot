package events.discordevents.guildmemberremove;

import java.util.Locale;
import commands.runnables.utilitycategory.WelcomeCommand;
import core.PermissionCheckRuntime;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.Welcome;
import mysql.modules.guild.DBGuild;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@DiscordEvent(allowBots = true)
public class GuildMemberRemoveWelcome extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(guild.getIdLong());
        if (welcomeMessageBean.isGoodbyeActive()) {
            welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
                    User user = event.getUser();
                    channel.sendMessage(
                            StringUtil.defuseMassPing(
                                    Welcome.resolveVariables(
                                            welcomeMessageBean.getGoodbyeText(),
                                            StringUtil.escapeMarkdown(guild.getName()),
                                            user.getAsMention(),
                                            StringUtil.escapeMarkdown(user.getName()),
                                            StringUtil.escapeMarkdown(user.getAsTag()),
                                            StringUtil.numToString(guild.getMemberCount())
                                    )
                            )
                    ).queue();
                }
            });
        }

        return true;
    }

}
