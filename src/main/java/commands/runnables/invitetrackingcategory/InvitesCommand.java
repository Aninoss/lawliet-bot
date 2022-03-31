package commands.runnables.invitetrackingcategory;

import java.util.Locale;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.atomicassets.AtomicMember;
import core.utils.StringUtil;
import modules.invitetracking.InviteMetrics;
import modules.invitetracking.InviteTracking;
import mysql.modules.invitetracking.DBInviteTracking;
import mysql.modules.invitetracking.InviteTrackingSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "invites",
        releaseDate = { 2021, 9, 21 },
        emoji = "✉️",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "serverinvites", "serverinvite", "inv" }
)
public class InvitesCommand extends MemberAccountAbstract {

    public InvitesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong()).isActive()) {
            return super.onTrigger(event, args);
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError(
                    this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "invites_notenabled_description").replace("{PREFIX}", getPrefix()),
                    TextManager.getString(getLocale(), TextManager.GENERAL, "invites_notenabled_title")
            );
            drawMessageNew(eb).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) {
        InviteMetrics inviteMetrics = InviteTracking.generateInviteMetrics(event.getGuild(), member.getIdLong());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .setTitle(null)
                .setAuthor(getString("template_title", member.getEffectiveName()), null, member.getUser().getEffectiveAvatarUrl())
                .setDescription(getString("template_desc",
                        StringUtil.numToString(inviteMetrics.getTotalInvites()),
                        StringUtil.numToString(inviteMetrics.getOnServer()),
                        StringUtil.numToString(inviteMetrics.getRetained()),
                        StringUtil.numToString(inviteMetrics.getActive()))
                );
        InviteTrackingSlot slot = DBInviteTracking.getInstance().retrieve(event.getGuild().getIdLong()).getInviteTrackingSlots().get(member.getIdLong());
        if (slot != null) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE, getString("invitedby", slot.getInviterUserId() == 0, new AtomicMember(event.getGuild().getIdLong(), slot.getInviterUserId()).getAsMention()), false);
        }
        return eb;
    }

}
