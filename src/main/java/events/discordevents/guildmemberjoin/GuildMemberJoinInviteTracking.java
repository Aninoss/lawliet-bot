package events.discordevents.guildmemberjoin;

import commands.Category;
import commands.runnables.invitetrackingcategory.InviteTrackingCommand;
import core.EmbedFactory;
import core.PermissionCheckRuntime;
import core.ShardManager;
import core.TextManager;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import modules.invitetracking.InviteMetrics;
import modules.invitetracking.InviteTracking;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.InviteTrackingEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinInviteTracking extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        InviteTrackingEntity inviteTracking = guildEntity.getInviteTracking();
        if (inviteTracking.getActive()) {
            Locale locale = guildEntity.getLocale();
            try {
                InviteTracking.TempInvite invite = InviteTracking.registerMemberJoin(guildEntity, event.getMember(), locale);
                sendLog(inviteTracking, locale, event.getMember(), invite);
            } catch (Throwable e) {
                //ignore
                sendLog(inviteTracking, locale, event.getMember(), null);
            }
        }
        return true;
    }

    private void sendLog(InviteTrackingEntity inviteTracking, Locale locale, Member member, InviteTracking.TempInvite invite) {
        GuildMessageChannel channel = inviteTracking.getLogChannel().get().orElse(null);
        if (channel == null || !PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            return;
        }

        String invitedName = StringUtil.escapeMarkdown(member.getUser().getName());
        String inviterName = "";
        String code = "";
        String uses = "";
        int n = 0;
        if (invite != null) {
            uses = StringUtil.numToString(invite.getUses());
            if (invite.getInviter() > 0) {
                User user = ShardManager.fetchUserById(invite.getInviter())
                        .exceptionally(e -> null)
                        .join();
                code = invite.getCode();
                if (user != null) {
                    inviterName = StringUtil.escapeMarkdown(user.getName());
                } else {
                    inviterName = String.valueOf(invite.getInviter());
                }
                n = 2;
            } else {
                n = 1;
            }
        }

        String created = TimeFormat.RELATIVE.atInstant(member.getTimeCreated().toInstant()).toString();
        String text = TextManager.getString(locale, Category.INVITE_TRACKING, "invitetracking_log", n, invitedName, created, inviterName, code, uses);

        MessageCreateAction messageAction;
        if (inviteTracking.getAdvanced()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setAuthor(member.getUser().getName(), null, member.getEffectiveAvatarUrl())
                    .setDescription(text);

            if (invite != null) {
                InviteMetrics inviteMetrics = InviteTracking.generateInviteMetrics(inviteTracking, member.getGuild(), invite.getInviter());
                String statsTitle = invite.getInviter() > 0
                        ? TextManager.getString(locale, Category.INVITE_TRACKING, "invites_template_title", inviterName)
                        : TextManager.getString(locale, TextManager.GENERAL, "invites_vanity");
                String stats = TextManager.getString(locale, Category.INVITE_TRACKING, "invites_template_desc",
                        StringUtil.numToString(inviteMetrics.getTotalInvites()),
                        StringUtil.numToString(inviteMetrics.getOnServer()),
                        StringUtil.numToString(inviteMetrics.getRetained()),
                        StringUtil.numToString(inviteMetrics.getActive())
                );
                eb.addField(statsTitle, stats, false);
            }

            messageAction = channel.sendMessageEmbeds(eb.build());
            if (inviteTracking.getPing()) {
                messageAction = messageAction.setContent(member.getAsMention());
            }
        } else {
            messageAction = channel.sendMessage(text);
            if (inviteTracking.getPing()) {
                messageAction = messageAction.setAllowedMentions(Set.of(Message.MentionType.USER));
            } else {
                messageAction = messageAction.setAllowedMentions(Collections.emptySet());
            }
        }
        messageAction.queue();
    }

}
