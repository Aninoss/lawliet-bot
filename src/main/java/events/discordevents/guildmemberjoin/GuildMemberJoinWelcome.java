package events.discordevents.guildmemberjoin;

import java.util.Locale;
import commands.runnables.utilitycategory.WelcomeCommand;
import commands.Category;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.modules.guild.DBGuild;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent(allowBots = true)
public class GuildMemberJoinWelcome extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();

        WelcomeMessageData welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(guild.getIdLong());
        if (welcomeMessageBean.isDmActive()) {
            sendDmMessage(event, welcomeMessageBean);
        }

        if (welcomeMessageBean.isWelcomeActive()) {
            welcomeMessageBean.getWelcomeChannel().ifPresent(channel -> {
                sendWelcomeMessage(event, welcomeMessageBean, channel, locale);
            });
        }

        return true;
    }

    private void sendDmMessage(GuildMemberJoinEvent event, WelcomeMessageData welcomeMessageBean) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String text = welcomeMessageBean.getDmText();

        if (text.length() > 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setAuthor(TextManager.getString(welcomeMessageBean.getGuildData().getLocale(), Category.UTILITY, "welcome_dm", guild.getName()), null, guild.getIconUrl())
                    .setDescription(
                            Welcome.resolveVariables(
                                    welcomeMessageBean.getDmText(),
                                    StringUtil.escapeMarkdown(guild.getName()),
                                    member.getAsMention(),
                                    StringUtil.escapeMarkdown(member.getEffectiveName()),
                                    StringUtil.escapeMarkdown(event.getUser().getAsTag()),
                                    StringUtil.numToString(guild.getMemberCount())
                            )
                    );

            JDAUtil.sendPrivateMessage(member, eb.build()).queue();
        }
    }

    private void sendWelcomeMessage(GuildMemberJoinEvent event, WelcomeMessageData welcomeMessageBean, TextChannel channel, Locale locale) {
        Guild guild = event.getGuild();

        if (PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
            WelcomeGraphics.createImageWelcome(event.getMember(), welcomeMessageBean.getWelcomeTitle())
                    .thenAccept(image -> {
                        Member member = event.getMember();
                        String content = Welcome.resolveVariables(
                                welcomeMessageBean.getWelcomeText(),
                                StringUtil.escapeMarkdown(guild.getName()),
                                event.getUser().getAsMention(),
                                StringUtil.escapeMarkdown(member.getEffectiveName()),
                                StringUtil.escapeMarkdown(event.getUser().getAsTag()),
                                StringUtil.numToString(guild.getMemberCount())
                        );

                        if (image != null) {
                            channel.sendMessage(content)
                                    .addFile(image, "welcome.png")
                                    .queue();
                        } else {
                            channel.sendMessage(content).queue();
                        }
                    });
        }
    }

}
