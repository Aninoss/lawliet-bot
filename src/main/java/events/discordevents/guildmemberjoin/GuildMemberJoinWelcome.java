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
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageData;
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
        Locale locale = entityManager.findGuildEntity(guild.getIdLong()).getLocale();

        WelcomeMessageData welcomeMessageData = DBWelcomeMessage.getInstance().retrieve(guild.getIdLong());
        if (welcomeMessageData.isDmActive()) {
            sendDmMessage(event, welcomeMessageData, locale);
        }

        if (welcomeMessageData.isWelcomeActive()) {
            welcomeMessageData.getWelcomeChannel()
                    .ifPresent(channel -> {
                        if (welcomeMessageData.getBanner()) {
                            generateBannerAndSendMessage(event.getMember(), welcomeMessageData, channel, locale);
                        } else {
                            sendMessage(event.getMember(), welcomeMessageData, channel, locale, null);
                        }
                    });
        }

        return true;
    }

    private void sendDmMessage(GuildMemberJoinEvent event, WelcomeMessageData welcomeMessageData, Locale locale) {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String text = welcomeMessageData.getDmText();

        if (!text.isEmpty()) {
            String content = Welcome.resolveVariables(
                    welcomeMessageData.getDmText(),
                    StringUtil.escapeMarkdown(guild.getName()),
                    member.getAsMention(),
                    StringUtil.escapeMarkdown(member.getUser().getName()),
                    StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                    StringUtil.numToString(guild.getMemberCount()),
                    StringUtil.escapeMarkdown(member.getUser().getEffectiveName())
            );

            if (welcomeMessageData.getDmEmbed()) {
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
    }

    private void generateBannerAndSendMessage(Member member, WelcomeMessageData welcomeMessageData, GuildMessageChannel channel, Locale locale) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)) {
            return;
        }

        WelcomeGraphics.createImageWelcome(member, welcomeMessageData.getWelcomeTitle())
                .thenAccept(image -> sendMessage(member, welcomeMessageData, channel, locale, image));
    }

    private void sendMessage(Member member, WelcomeMessageData welcomeMessageData, GuildMessageChannel channel, Locale locale, InputStream image) {
        if (!PermissionCheckRuntime.botHasPermission(locale, WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        Guild guild = member.getGuild();
        String content = Welcome.resolveVariables(
                welcomeMessageData.getWelcomeText(),
                StringUtil.escapeMarkdown(guild.getName()),
                member.getAsMention(),
                StringUtil.escapeMarkdown(member.getUser().getName()),
                StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                StringUtil.numToString(guild.getMemberCount()),
                StringUtil.escapeMarkdown(member.getUser().getEffectiveName())
        );

        if (welcomeMessageData.getWelcomeEmbed()) {
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
