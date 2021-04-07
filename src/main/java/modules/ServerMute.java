package modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.runnables.moderationcategory.MuteCommand;
import core.PermissionCheckRuntime;
import mysql.modules.guild.DBGuild;
import mysql.modules.moderation.DBModeration;
import mysql.modules.servermute.DBServerMute;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class ServerMute {

    public static void process(Member member) {
        Guild guild = member.getGuild();
        Locale locale = DBGuild.getInstance().retrieve(guild.getIdLong()).getLocale();

        if (DBServerMute.getInstance().retrieve(guild.getIdLong()).containsKey(member.getIdLong())) {
            DBModeration.getInstance().retrieve(guild.getIdLong()).getMuteRole().ifPresent(muteRole -> {
                if (PermissionCheckRuntime.getInstance().botCanManageRoles(locale, MuteCommand.class, muteRole)) {
                    guild.addRoleToMember(member, muteRole)
                            .reason(Command.getCommandLanguage(MuteCommand.class, locale).getTitle())
                            .queue();
                }
            });
        }
    }

    public static List<TextChannel> getLeakedChannels(Guild guild) {
        ArrayList<TextChannel> leakedChannels = new ArrayList<>();
        DBModeration.getInstance().retrieve(guild.getIdLong()).getMuteRole().ifPresent(muteRole -> {
            for (TextChannel channel : guild.getTextChannels()) {
                PermissionOverride publicOverride = channel.getPermissionOverride(guild.getPublicRole());

                /* ignore channel if no one except for administrators has message read permissions */
                if (publicOverride != null &&
                        publicOverride.getDenied().contains(Permission.MESSAGE_READ) &&
                        channel.getRolePermissionOverrides().stream().noneMatch(o -> o.getAllowed().contains(Permission.MESSAGE_READ))
                ) {
                    continue;
                }

                /* add channel if any overridden role permission allows message write */
                if (channel.getRolePermissionOverrides().stream().anyMatch(o -> o.getAllowed().contains(Permission.MESSAGE_WRITE))) {
                    leakedChannels.add(channel);
                    continue;
                }

                /* ignore channel if no one except for administrators has message write permissions */
                if (publicOverride != null && publicOverride.getDenied().contains(Permission.MESSAGE_WRITE)) {
                    continue;
                }

                /* add channel if mute role doesn't deny message write permissions */
                PermissionOverride permissionOverride = channel.getPermissionOverride(muteRole);
                if (permissionOverride == null || !permissionOverride.getDenied().contains(Permission.MESSAGE_WRITE)) {
                    leakedChannels.add(channel);
                }
            }
        });

        return leakedChannels;
    }


}
