package events.discordevents.guildmemberremove;

import commands.runnables.configurationcategory.WelcomeCommand;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberRemoveAbstract;
import modules.Welcome;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;

import java.util.HashSet;
import java.util.Locale;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRemoveWelcome extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) {
        Guild guild = event.getGuild();
        Locale locale = entityManager.findGuildEntity(guild.getIdLong()).getLocale();

        WelcomeMessageData welcomeMessageData = DBWelcomeMessage.getInstance().retrieve(guild.getIdLong());
        if (welcomeMessageData.isGoodbyeActive()) {
            welcomeMessageData.getGoodbyeChannel().ifPresent(channel -> {
                if (PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                    User user = event.getUser();
                    String content = Welcome.resolveVariables(
                            welcomeMessageData.getGoodbyeText(),
                            StringUtil.escapeMarkdown(guild.getName()),
                            user.getAsMention(),
                            StringUtil.escapeMarkdown(user.getName()),
                            StringUtil.escapeMarkdown(user.getAsTag()),
                            StringUtil.numToString(guild.getMemberCount()),
                            StringUtil.escapeMarkdown(user.getEffectiveName())
                    );

                    if (welcomeMessageData.getGoodbyeEmbed()) {
                        HashSet<String> userMentions = MentionUtil.extractUserMentions(content);
                        StringBuilder sb = new StringBuilder();
                        userMentions.forEach(mention -> sb.append(mention).append(" "));

                        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                                .setDescription(content)
                                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));

                        channel.sendMessage(sb.toString())
                                .addEmbeds(eb.build())
                                .queue();
                    } else {
                        EmbedBuilder eb = EmbedFactory.getWrittenByServerStaffEmbed(locale);
                        channel.sendMessage(content)
                                .addEmbeds(eb.build())
                                .queue();
                    }
                }
            });
        }

        return true;
    }

}
