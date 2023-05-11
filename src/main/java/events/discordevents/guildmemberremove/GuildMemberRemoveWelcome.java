package events.discordevents.guildmemberremove;

import java.util.Locale;
import commands.Category;
import commands.runnables.utilitycategory.WelcomeCommand;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.Welcome;
import mysql.modules.guild.DBGuild;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRemoveWelcome extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();

        WelcomeMessageData welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(guild.getIdLong());
        if (welcomeMessageBean.isGoodbyeActive()) {
            welcomeMessageBean.getGoodbyeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                    User user = event.getUser();
                    EmbedBuilder eb = new EmbedBuilder()
                            .setDescription(TextManager.getString(locale, Category.UTILITY, "welcome_action_text"));

                    channel.sendMessage(
                                    Welcome.resolveVariables(
                                            welcomeMessageBean.getGoodbyeText(),
                                            StringUtil.escapeMarkdown(guild.getName()),
                                            user.getAsMention(),
                                            StringUtil.escapeMarkdown(user.getName()),
                                            StringUtil.escapeMarkdown(user.getAsTag()),
                                            StringUtil.numToString(guild.getMemberCount())
                                    )
                            )
                            .addEmbeds(eb.build())
                            .queue();
                }
            });
        }

        return true;
    }

}
