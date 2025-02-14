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
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesLeaveEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;
import java.util.HashSet;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberRemoveWelcome extends GuildMemberRemoveAbstract {

    @Override
    public boolean onGuildMemberRemove(GuildMemberRemoveEvent event, EntityManagerWrapper entityManager) {
        Guild guild = event.getGuild();
        GuildEntity guildEntity = entityManager.findGuildEntity(guild.getIdLong());

        WelcomeMessagesLeaveEntity leave = guildEntity.getWelcomeMessages().getLeave();
        if (!leave.getActive()) {
            return true;
        }

        leave.getChannel().get().ifPresent(channel -> {
            if (PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), WelcomeCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                User user = event.getUser();
                String content = Welcome.resolveVariables(
                        leave.getText(),
                        StringUtil.escapeMarkdown(guild.getName()),
                        user.getAsMention(),
                        StringUtil.escapeMarkdown(user.getName()),
                        StringUtil.escapeMarkdown(user.getAsTag()),
                        StringUtil.numToString(guild.getMemberCount()),
                        StringUtil.escapeMarkdown(user.getEffectiveName())
                );

                if (leave.getEmbeds()) {
                    HashSet<String> userMentions = MentionUtil.extractUserMentions(content);
                    StringBuilder sb = new StringBuilder();
                    userMentions.forEach(mention -> sb.append(mention).append(" "));

                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setDescription(content)
                            .setFooter(TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "serverstaff_text"))
                            .setImage(leave.retrieveRandomImageUrl());

                    channel.sendMessage(sb.toString())
                            .addEmbeds(eb.build())
                            .queue();
                } else {
                    content = StringUtil.addWrittenByServerStaffDisclaimer(content, guildEntity.getLocale(), Message.MAX_CONTENT_LENGTH);
                    MessageCreateAction messageCreateAction = channel.sendMessage(content);
                    File imageFile = leave.retrieveRandomImageFile();
                    if (imageFile != null) {
                        messageCreateAction.addFiles(FileUpload.fromData(imageFile));
                    }
                    messageCreateAction.queue();
                }
            }
        });

        return true;
    }

}
