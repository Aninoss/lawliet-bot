package events.discordevents.guildmemberjoin;

import java.io.InputStream;
import java.util.Locale;
import commands.Category;
import commands.runnables.utilitycategory.WelcomeCommand;
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
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

@DiscordEvent(allowBots = true, allowBannedUser = true)
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
            welcomeMessageBean.getWelcomeChannel()
                    .ifPresent(channel -> generateBannerAndSendMessage(event.getMember(), welcomeMessageBean, channel, locale));
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

            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();
        }
    }

    private void generateBannerAndSendMessage(Member member, WelcomeMessageData welcomeMessageData, TextChannel channel, Locale locale) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
            return;
        }

        WelcomeGraphics.createImageWelcome(member, welcomeMessageData.getWelcomeTitle())
                .thenAccept(image -> sendMessage(member, welcomeMessageData, channel, locale, image));
    }

    private void sendMessage(Member member, WelcomeMessageData welcomeMessageData, TextChannel channel, Locale locale, InputStream image) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        Guild guild = member.getGuild();
        String content = Welcome.resolveVariables(
                welcomeMessageData.getWelcomeText(),
                StringUtil.escapeMarkdown(guild.getName()),
                member.getAsMention(),
                StringUtil.escapeMarkdown(member.getEffectiveName()),
                StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                StringUtil.numToString(guild.getMemberCount())
        );

        MessageCreateAction messageCreateAction = channel.sendMessage(content);
        if (image != null) {
            messageCreateAction.addFiles(FileUpload.fromData(image, "welcome.png"));
        }

        EmbedBuilder eb = new EmbedBuilder()
                .setDescription(TextManager.getString(locale, Category.UTILITY, "welcome_action_text"));
        messageCreateAction.addEmbeds(eb.build())
                .queue();
    }

}
