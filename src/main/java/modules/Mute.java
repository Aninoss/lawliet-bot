package modules;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import commands.Command;
import commands.runnables.moderationcategory.ModSettingsCommand;
import core.MemberCacheController;
import core.PermissionCheckRuntime;
import core.utils.BotPermissionUtil;
import modules.schedulers.ServerMuteScheduler;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

public class Mute {

    public static void mute(Guild guild, User target, long minutes, String reason) {
        ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
        if (prerequisites(guild, moderationBean)) {
            Instant expiration = minutes > 0 ? Instant.now().plus(Duration.ofMinutes(minutes)) : null;
            ServerMuteData serverMuteData = new ServerMuteData(guild.getIdLong(), target.getIdLong(), expiration);
            DBServerMute.getInstance().retrieve(guild.getIdLong()).put(target.getIdLong(), serverMuteData);
            ServerMuteScheduler.loadServerMute(serverMuteData);

            Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
            if (member != null) {
                moderationBean.getMuteRole().ifPresent(muteRole -> {
                    guild.addRoleToMember(member, muteRole)
                            .reason(reason)
                            .queue();
                });
            }
        }
    }

    public static void unmute(Guild guild, User target, String reason) {
        ModerationData moderationBean = DBModeration.getInstance().retrieve(guild.getIdLong());
        if (prerequisites(guild, moderationBean)) {
            DBServerMute.getInstance().retrieve(guild.getIdLong())
                    .remove(target.getIdLong());

            Member member = MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).join();
            if (member != null) {
                moderationBean.getMuteRole().ifPresent(muteRole -> {
                    guild.removeRoleFromMember(member, muteRole)
                            .reason(reason)
                            .queue();
                });
            }
        }
    }

    private static boolean prerequisites(Guild guild, ModerationData moderationBean) {
        Optional<Role> muteRoleOpt = moderationBean.getMuteRole();
        return muteRoleOpt.isPresent() && guild.getSelfMember().canInteract(muteRoleOpt.get());
    }

    public static List<TextChannel> getLeakedChannels(Guild guild) {
        ArrayList<TextChannel> leakedChannels = new ArrayList<>();
        DBModeration.getInstance().retrieve(guild.getIdLong()).getMuteRole().ifPresent(muteRole -> {
            for (TextChannel channel : guild.getTextChannels()) {
                PermissionOverride publicOverride = channel.getPermissionOverride(guild.getPublicRole());

                /* ignore channel if no one except for administrators has message read permissions */
                if (publicOverride != null &&
                        publicOverride.getDenied().contains(Permission.VIEW_CHANNEL) &&
                        channel.getRolePermissionOverrides().stream().noneMatch(o -> o.getAllowed().contains(Permission.VIEW_CHANNEL))
                ) {
                    continue;
                }

                /* add channel if any overridden role permission allows message write */
                if (channel.getRolePermissionOverrides().stream().anyMatch(o -> o.getAllowed().contains(Permission.MESSAGE_SEND))) {
                    leakedChannels.add(channel);
                    continue;
                }

                /* ignore channel if no one except for administrators has message write permissions */
                if (publicOverride != null && publicOverride.getDenied().contains(Permission.MESSAGE_SEND)) {
                    continue;
                }

                /* add channel if mute role doesn't deny message write permissions */
                PermissionOverride permissionOverride = channel.getPermissionOverride(muteRole);
                if (permissionOverride == null || !permissionOverride.getDenied().contains(Permission.MESSAGE_SEND)) {
                    leakedChannels.add(channel);
                }
            }
        });

        return leakedChannels;
    }

    public static void enforceMuteRole(Guild guild) {
        ModerationData moderationData = DBModeration.getInstance().retrieve(guild.getIdLong());
        Role role = moderationData.getMuteRole().orElse(null);
        if (role != null && moderationData.getEnforceMuteRoleEffectively()) {
            Locale locale = moderationData.getGuildData().getLocale();
            guild.getCategories().forEach(category -> enforceMuteOnGuildChannel(locale, role, category));
            guild.getTextChannels().forEach(channel -> enforceMuteOnGuildChannel(locale, role, channel));
            guild.getNewsChannels().forEach(channel -> enforceMuteOnGuildChannel(locale, role, channel));
        }
    }

    private static void enforceMuteOnGuildChannel(Locale locale, Role muteRole, IPermissionContainer guildChannel) {
        PermissionOverride permissionOverride = guildChannel.getPermissionContainer().getPermissionOverride(muteRole);
        if ((permissionOverride == null || !permissionOverride.getDenied().contains(Permission.MESSAGE_SEND)) &&
                PermissionCheckRuntime.botHasPermission(locale, ModSettingsCommand.class, guildChannel.getGuild(), Permission.ADMINISTRATOR)
        ) {
            BotPermissionUtil.addPermission(guildChannel, guildChannel.getManager(), muteRole, false, Permission.MESSAGE_SEND)
                    .reason(Command.getCommandLanguage(ModSettingsCommand.class, locale).getTitle())
                    .queue();
        }
    }

}
