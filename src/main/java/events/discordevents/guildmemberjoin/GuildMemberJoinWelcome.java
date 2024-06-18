package events.discordevents.guildmemberjoin;

import commands.runnables.configurationcategory.WelcomeCommand;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesDmEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesJoinEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinWelcome extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) {
        Guild guild = event.getGuild();
        GuildEntity guildEntity = entityManager.findGuildEntity(guild.getIdLong());
        WelcomeMessagesEntity welcomeMessages = guildEntity.getWelcomeMessages();

        if (welcomeMessages.getDm().getActive()) {
            sendDmMessage(event, welcomeMessages.getDm(), guildEntity.getLocale());
        }

        if (welcomeMessages.getJoin().getActive()) {
            WelcomeMessagesJoinEntity join = welcomeMessages.getJoin();
            join.getChannel().get()
                    .ifPresent(channel -> {
                        if (join.getImageMode() == WelcomeMessagesJoinEntity.ImageMode.GENERATED_BANNERS) {
                            generateBannerAndSendMessage(event.getMember(), join, channel, guildEntity.getLocale());
                        } else {
                            sendMessage(event.getMember(), join, channel, guildEntity.getLocale(), null);
                        }
                    });
        }

        return true;
    }

    private void sendDmMessage(GuildMemberJoinEvent event, WelcomeMessagesDmEntity dm, Locale locale) {
        if (dm.getText().isEmpty()) {
            return;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();
        String content = Welcome.resolveVariables(
                dm.getText(),
                StringUtil.escapeMarkdown(guild.getName()),
                member.getAsMention(),
                StringUtil.escapeMarkdown(member.getUser().getName()),
                StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                StringUtil.numToString(guild.getMemberCount()),
                StringUtil.escapeMarkdown(member.getUser().getEffectiveName())
        );

        if (dm.getEmbeds()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setDescription(content)
                    .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text_server", event.getGuild().getName()));

            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();
        } else {
            EmbedBuilder eb = EmbedFactory.getWrittenByServerStaffEmbed(event.getGuild(), locale);
            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessage(content)
                            .addEmbeds(eb.build())
                    )
                    .queue();
        }
    }

    private void generateBannerAndSendMessage(Member member, WelcomeMessagesJoinEntity join, GuildMessageChannel channel, Locale locale) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
            return;
        }

        WelcomeGraphics.createImageWelcome(member, join.getBannerTitle())
                .thenAccept(image -> sendMessage(member, join, channel, locale, image));
    }

    private void sendMessage(Member member, WelcomeMessagesJoinEntity join, GuildMessageChannel channel, Locale locale, InputStream image) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        Guild guild = member.getGuild();
        String content = Welcome.resolveVariables(
                join.getText(),
                StringUtil.escapeMarkdown(guild.getName()),
                member.getAsMention(),
                StringUtil.escapeMarkdown(member.getUser().getName()),
                StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                StringUtil.numToString(guild.getMemberCount()),
                StringUtil.escapeMarkdown(member.getUser().getEffectiveName())
        );

        if (join.getEmbeds()) {
            HashSet<String> userMentions = MentionUtil.extractUserMentions(content);
            StringBuilder sb = new StringBuilder();
            userMentions.forEach(mention -> sb.append(mention).append(" "));

            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setDescription(content)
                    .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));

            if (image != null) {
                eb.setImage("attachment://welcome.png");
                channel.sendMessageEmbeds(eb.build())
                        .setContent(sb.toString())
                        .addFiles(FileUpload.fromData(image, "welcome.png"))
                        .queue();
            } else {
                channel.sendMessageEmbeds(eb.build())
                        .setContent(sb.toString())
                        .queue();
            }
        } else {
            MessageCreateAction messageCreateAction = channel.sendMessage(content);
            if (image != null) {
                messageCreateAction.addFiles(FileUpload.fromData(image, "welcome.png"));
            }

            EmbedBuilder eb = EmbedFactory.getWrittenByServerStaffEmbed(locale);
            messageCreateAction.addEmbeds(eb.build())
                    .queue();
        }
    }

}
