package modules.moderation;

import commands.Command;
import commands.CommandEvent;
import commands.runnables.moderationcategory.LockCommand;
import commands.runnables.moderationcategory.UnlockCommand;
import core.ShardManager;
import core.utils.CommandUtil;
import modules.schedulers.ChannelLockScheduler;
import mysql.hibernate.entity.guild.ChannelLockEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.attribute.IPermissionContainer;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.managers.channel.attribute.IPermissionContainerManager;

import java.time.Duration;
import java.time.Instant;

public class ChannelLock {

    public static GuildMessageChannel getChannel(Command command, CommandEvent event, String args) {
        CommandUtil.ChannelResponse response = CommandUtil.differentChannelExtract(command, event, event.getMessageChannel(), args, Permission.MESSAGE_SEND, Permission.MANAGE_PERMISSIONS);
        if (response != null) {
            return response.getChannel();
        } else {
            return null;
        }
    }

    public static void lock(GuildEntity guildEntity, GuildMessageChannel channel, long logChannelId, long minutes) {
        IPermissionContainer permissionContainer = channel.getPermissionContainer();
        IPermissionContainerManager<?, ?> manager = permissionContainer.getManager();

        ChannelLockEntity channelLock = new ChannelLockEntity(channel.getGuild().getIdLong(), channel.getIdLong());
        PermissionOverride selfPermissionOverride = permissionContainer.getPermissionOverride(channel.getGuild().getSelfMember());
        if (selfPermissionOverride != null) {
            if (selfPermissionOverride.getAllowed().contains(Permission.MESSAGE_SEND)) {
                channelLock.setModifiedSelfWritePermission(false);
            } else {
                manager = manager.putPermissionOverride(
                        channel.getGuild().getSelfMember(),
                        selfPermissionOverride.getAllowedRaw() | Permission.MESSAGE_SEND.getRawValue(),
                        selfPermissionOverride.getDeniedRaw() & ~Permission.MESSAGE_SEND.getRawValue()
                );
            }
        } else {
            manager = manager.putPermissionOverride(
                    channel.getGuild().getSelfMember(),
                    Permission.MESSAGE_SEND.getRawValue(),
                    0L
            );
        }

        for (PermissionOverride permissionOverride : permissionContainer.getPermissionOverrides()) {
            if (permissionOverride.getDenied().contains(Permission.MESSAGE_SEND) || permissionOverride.getIdLong() == ShardManager.getSelfId()) {
                continue;
            }

            channelLock.getEntityIds().put(permissionOverride.getIdLong(), permissionOverride.getAllowed().contains(Permission.MESSAGE_SEND));
            if (permissionOverride.isMemberOverride()) {
                manager = manager.putMemberPermissionOverride(
                        permissionOverride.getIdLong(),
                        permissionOverride.getAllowedRaw() & ~Permission.MESSAGE_SEND.getRawValue(),
                        permissionOverride.getDeniedRaw() | Permission.MESSAGE_SEND.getRawValue()
                );
            } else {
                manager = manager.putRolePermissionOverride(
                        permissionOverride.getIdLong(),
                        permissionOverride.getAllowedRaw() & ~Permission.MESSAGE_SEND.getRawValue(),
                        permissionOverride.getDeniedRaw() | Permission.MESSAGE_SEND.getRawValue()
                );
            }
        }

        channelLock.setLogChannelId(logChannelId);
        if (minutes > 0) {
            channelLock.setUntil(Instant.now().plus(Duration.ofMinutes(minutes)));
            ChannelLockScheduler.loadChannelLock(channelLock);
        }

        manager.reason(Command.getCommandLanguage(LockCommand.class, guildEntity.getLocale()).getTitle())
                .complete();
        guildEntity.getChannelLocks().put(channel.getIdLong(), channelLock);
    }

    public static void unlock(GuildEntity guildEntity, GuildMessageChannel channel) {
        IPermissionContainer permissionContainer = channel.getPermissionContainer();
        IPermissionContainerManager<?, ?> manager = permissionContainer.getManager();

        ChannelLockEntity channelLock = guildEntity.getChannelLocks().get(channel.getIdLong());
        PermissionOverride selfPermissionOverride = permissionContainer.getPermissionOverride(channel.getGuild().getSelfMember());
        if (selfPermissionOverride != null && channelLock.getModifiedSelfWritePermission()) {
            long allow = selfPermissionOverride.getAllowedRaw() & ~Permission.MESSAGE_SEND.getRawValue();
            long deny = selfPermissionOverride.getDeniedRaw() & ~Permission.MESSAGE_SEND.getRawValue();
            if (allow == 0L && deny == 0L) {
                manager = manager.removePermissionOverride(channel.getGuild().getSelfMember());
            } else {
                manager = manager.putPermissionOverride(
                        channel.getGuild().getSelfMember(),
                        allow,
                        deny
                );
            }
        }

        for (PermissionOverride permissionOverride : permissionContainer.getPermissionOverrides()) {
            if (!permissionOverride.getDenied().contains(Permission.MESSAGE_SEND) || !channelLock.getEntityIds().containsKey(permissionOverride.getIdLong())) {
                continue;
            }

            boolean allow = channelLock.getEntityIds().get(permissionOverride.getIdLong());
            if (permissionOverride.isMemberOverride()) {
                manager = manager.putMemberPermissionOverride(
                        permissionOverride.getIdLong(),
                        permissionOverride.getAllowedRaw() | (allow ? Permission.MESSAGE_SEND.getRawValue() : 0L),
                        permissionOverride.getDeniedRaw() & ~Permission.MESSAGE_SEND.getRawValue()
                );
            } else {
                manager = manager.putRolePermissionOverride(
                        permissionOverride.getIdLong(),
                        permissionOverride.getAllowedRaw() | (allow ? Permission.MESSAGE_SEND.getRawValue() : 0L),
                        permissionOverride.getDeniedRaw() & ~Permission.MESSAGE_SEND.getRawValue()
                );
            }
        }
        manager.reason(Command.getCommandLanguage(UnlockCommand.class, guildEntity.getLocale()).getTitle())
                .complete();
        guildEntity.getChannelLocks().remove(channel.getIdLong());
    }

}
