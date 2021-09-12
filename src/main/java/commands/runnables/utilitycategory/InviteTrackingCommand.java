package commands.runnables.utilitycategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import modules.invitetracking.InviteTracking;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

@CommandProperties(
        trigger = "invitetracking",
        userGuildPermissions = Permission.MANAGE_SERVER,
        botGuildPermissions = Permission.MANAGE_SERVER,
        releaseDate = { 2021, 9, 20 },
        emoji = "✉️",
        usesExtEmotes = true,
        executableWithoutArgs = true,
        aliases = { "invtracking", "invitet", "invt", "invtracker" }
)
public class InviteTrackingCommand extends CommandOnOffSwitchAbstract {

    public InviteTrackingCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

    @Override
    protected boolean isActive(Member member) {
        return DBGuild.getInstance().retrieve(member.getGuild().getIdLong()).isInviteTracking();
    }

    @Override
    protected boolean setActive(Member member, boolean active) {
        DBGuild.getInstance().retrieve(member.getGuild().getIdLong()).setInviteTracking(active);
        if (active) {
            InviteTracking.synchronizeGuildInvites(member.getGuild());
        }
        return true;
    }

}
