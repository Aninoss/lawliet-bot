package modules.mute;

import java.util.*;
import commands.Command;
import commands.runnables.moderationcategory.ChannelMuteCommand;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.managers.ChannelManager;

public class MuteManager {

    private static final MuteManager instance = new MuteManager();

    private MuteManager() {
    }

    public static MuteManager getInstance() {
        return instance;
    }

    public boolean executeMute(MuteData muteData, boolean mute) {
        return updatePermissions(muteData, mute);
    }

    private boolean updatePermissions(MuteData muteData, boolean mute) {
        TextChannel channel = muteData.getTextChannel().get();
        Locale locale = DBGuild.getInstance().retrieve(channel.getGuild().getIdLong()).getLocale();
        ChannelManager manager = channel.getManager();
        ArrayList<Member> members = muteData.getMembers();
        List<PermissionOverride> userPermissions = channel.getMemberPermissionOverrides();
        boolean updated = false;

        for (Member member : members) {
            Optional<PermissionOverride> permissionOverrideOpt = userPermissions.stream()
                    .filter(p -> p.getIdLong() == member.getIdLong()).findFirst();

            if (permissionOverrideOpt.isPresent()) {
                PermissionOverride permissionOverride = permissionOverrideOpt.get();
                if (mute && !permissionOverride.getDenied().contains(Permission.MESSAGE_WRITE)) {
                    manager = manager.putPermissionOverride(member,
                            permissionOverride.getAllowedRaw(),
                            permissionOverride.getDeniedRaw() | Permission.MESSAGE_WRITE.getRawValue()
                    );
                    updated = true;
                } else if (!mute && permissionOverride.getDenied().contains(Permission.MESSAGE_WRITE)) {
                    if (permissionOverride.getDeniedRaw() == Permission.MESSAGE_WRITE.getRawValue() &&
                            permissionOverride.getAllowed().size() == 0
                    ) {
                        manager = manager.removePermissionOverride(member);
                        updated = true;
                    } else {
                        manager = manager.putPermissionOverride(
                                member,
                                permissionOverride.getAllowedRaw(),
                                permissionOverride.getDeniedRaw() & ~Permission.MESSAGE_WRITE.getRawValue()
                        );
                        updated = true;
                    }
                }
            } else if (mute) {
                manager = manager.putPermissionOverride(member, Collections.emptyList(), Collections.singleton(Permission.MESSAGE_WRITE));
                updated = true;
            }
        }

        if (updated)
            manager.reason(Command.getCommandLanguage(ChannelMuteCommand.class, locale).getTitle()).queue();

        return updated;
    }

}
