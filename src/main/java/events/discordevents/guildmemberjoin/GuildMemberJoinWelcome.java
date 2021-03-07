package events.discordevents.guildmemberjoin;

import commands.runnables.utilitycategory.WelcomeCommand;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.modules.guild.DBGuild;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import java.io.InputStream;
import java.util.Locale;

@DiscordEvent(allowBots = true)
public class GuildMemberJoinWelcome extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        Guild guild = event.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();

        WelcomeMessageBean welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(guild.getIdLong());
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

    private void sendDmMessage(GuildMemberJoinEvent event, WelcomeMessageBean welcomeMessageBean) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String text = welcomeMessageBean.getDmText();

        if (text.length() > 0) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setAuthor(guild.getName(), "", guild.getIconUrl())
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

    private void sendWelcomeMessage(GuildMemberJoinEvent event, WelcomeMessageBean welcomeMessageBean, TextChannel channel, Locale locale) {
        Guild guild = event.getGuild();

        if (PermissionCheckRuntime.getInstance().botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
            InputStream image = WelcomeGraphics.createImageWelcome(event.getMember(), welcomeMessageBean.getWelcomeTitle());
            Member member = event.getMember();
            String content = StringUtil.defuseMassPing(
                    Welcome.resolveVariables(
                            welcomeMessageBean.getWelcomeText(),
                            StringUtil.escapeMarkdown(guild.getName()),
                            event.getUser().getAsMention(),
                            StringUtil.escapeMarkdown(member.getEffectiveName()),
                            StringUtil.escapeMarkdown(event.getUser().getAsTag()),
                            StringUtil.numToString(guild.getMemberCount())
                    )
            );

            if (image != null) {
                channel.sendMessage(content).addFile(image, "welcome.png").queue();
            } else {
                channel.sendMessage(content).queue();
            }
        }
    }

}
