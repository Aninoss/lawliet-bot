package events.discordevents.guildmemberjoin;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
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
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.TimeFormat;

@DiscordEvent(allowBots = true, allowBannedUser = true)
public class GuildMemberJoinInviteTracking extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        InviteTrackingData inviteTrackingData = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong());
        if (inviteTrackingData.isActive()) {
            GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
            Locale locale = guildEntity.getLocale();
            InviteTracking.registerMemberJoin(event.getMember(), locale)
                    .thenAccept(invite -> sendLog(inviteTrackingData, locale, event.getMember(), invite))
                    .exceptionally(e -> {
                        //ignore
                        sendLog(inviteTrackingData, locale, event.getMember(), null);
                        return null;
                    });
        }
        return true;
    }

    private void sendLog(InviteTrackingData inviteTrackingData, Locale locale, Member member, InviteTracking.TempInvite invite) {
        inviteTrackingData.getTextChannel().ifPresent(channel -> {
            if (PermissionCheckRuntime.botHasPermission(locale, InviteTrackingCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                String invitedTag = StringUtil.escapeMarkdown(member.getUser().getAsTag());
                String inviterTag = "";
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
                            inviterTag = StringUtil.escapeMarkdown(user.getAsTag());
                        } else {
                            inviterName = String.valueOf(invite.getInviter());
                            inviterTag = String.valueOf(invite.getInviter());
                        }
                        n = 2;
                    } else {
                        n = 1;
                    }
                }

                String created = TimeFormat.RELATIVE.atInstant(member.getTimeCreated().toInstant()).toString();
                String text = TextManager.getString(locale, Category.INVITE_TRACKING, "invitetracking_log", n, invitedTag, created, inviterTag, code, uses);

                MessageCreateAction messageAction;
                if (inviteTrackingData.isAdvanced()) {
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setAuthor(member.getUser().getAsTag(), null, member.getEffectiveAvatarUrl())
                            .setDescription(text);

                    if (invite != null) {
                        InviteMetrics inviteMetrics = InviteTracking.generateInviteMetrics(member.getGuild(), invite.getInviter());
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
                    if (inviteTrackingData.getPing()) {
                        messageAction = messageAction.setContent(member.getAsMention());
                    }
                } else {
                    messageAction = channel.sendMessage(text);
                    if (inviteTrackingData.getPing()) {
                        messageAction = messageAction.setAllowedMentions(Set.of(Message.MentionType.USER));
                    } else {
                        messageAction = messageAction.setAllowedMentions(Collections.emptySet());
                    }
                }
                messageAction.queue();
            }
        });
    }

}
