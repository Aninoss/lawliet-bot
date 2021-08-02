package modules;

import java.util.ArrayList;
import java.util.List;
import mysql.modules.moderation.DBModeration;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.TextChannel;

public class ServerMute {

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
