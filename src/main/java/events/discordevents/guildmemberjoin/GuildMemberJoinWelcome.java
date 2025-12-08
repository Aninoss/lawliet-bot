package events.discordevents.guildmemberjoin;

import commands.runnables.configurationcategory.WelcomeCommand;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.TextManager;
import core.utils.FileUtil;
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
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
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
                    .ifPresent(channel -> sendMessage(event.getMember(), join, channel, guildEntity.getLocale()));
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
                    .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text_server", event.getGuild().getName()))
                    .setImage(dm.retrieveRandomImageUrl());

            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();
        } else {
            File imageFile = dm.retrieveRandomImageFile();
            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> {
                                String newContent = StringUtil.addWrittenByServerStaffDisclaimer(content, locale, event.getGuild(), Message.MAX_CONTENT_LENGTH);
                                MessageCreateAction messageCreateAction = messageChannel.sendMessage(newContent);
                                if (imageFile != null && FileUtil.checkFileSizeConstraint(imageFile)) {
                                    messageCreateAction.addFiles(FileUpload.fromData(imageFile));
                                }
                                return messageCreateAction;
                            }
                    )
                    .queue();
        }
    }

    private void sendMessage(Member member, WelcomeMessagesJoinEntity join, GuildMessageChannel channel, Locale locale) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
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

            MessageCreateAction messageCreateAction = channel.sendMessage(sb.toString());
            switch (join.getAttachmentType()) {
                case GENERATED_BANNERS -> {
                    eb.setImage("attachment://welcome.png");
                    InputStream inputStream = WelcomeGraphics.createImageWelcome(member, join.getBannerTitle()).join();
                    messageCreateAction.addFiles(FileUpload.fromData(inputStream, "welcome.png"));
                }
                case IMAGE -> eb.setImage(join.retrieveRandomImageUrl());
            }
            messageCreateAction.addEmbeds(eb.build())
                    .queue();
        } else {
            content = StringUtil.addWrittenByServerStaffDisclaimer(content, locale, Message.MAX_CONTENT_LENGTH);
            MessageCreateAction messageCreateAction = channel.sendMessage(content);
            switch (join.getAttachmentType()) {
                case GENERATED_BANNERS -> {
                    InputStream inputStream = WelcomeGraphics.createImageWelcome(member, join.getBannerTitle()).join();
                    messageCreateAction.addFiles(FileUpload.fromData(inputStream, "welcome.png"));
                }
                case IMAGE -> {
                    File imageFile = join.retrieveRandomImageFile();
                    if (imageFile != null && FileUtil.checkFileSizeConstraint(imageFile)) {
                        messageCreateAction.addFiles(FileUpload.fromData(imageFile));
                    }
                }
            }
            messageCreateAction.queue();
        }
    }

}
