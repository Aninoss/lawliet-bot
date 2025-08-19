package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.LockCommand;
import commands.runnables.moderationcategory.UnlockCommand;
import core.*;
import core.atomicassets.AtomicGuildMessageChannel;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import modules.moderation.ChannelLock;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.ChannelLockEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.time.Instant;
import java.util.Map;

public class ChannelLockScheduler {

    public static void start() {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(ChannelLockScheduler.class)) {
            entityManager.findAllForResponsibleIds(ChannelLockEntity.class, "guildId")
                    .forEach(channelLock -> {
                        if (channelLock.getUntil() != null) {
                            loadChannelLock(channelLock);
                        }
                    });
        } catch (Throwable e) {
            MainLogger.get().error("Could not start channel locks", e);
        }
    }

    public static void loadChannelLock(ChannelLockEntity channelLock) {
        loadChannelLock(channelLock.getGuildId(), channelLock.getChannelId(), channelLock.getUntil());
    }

    public static void loadChannelLock(long guildId, long channelId, Instant until) {
        MainScheduler.schedule(until, () -> {
            try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId, ChannelLockScheduler.class)) {
                Map<Long, ChannelLockEntity> channelLocks = guildEntity.getChannelLocks();
                if (ShardManager.guildIsManaged(guildId) && channelLocks.containsKey(channelId)) {
                    onChannelLockDue(channelLocks.get(channelId), guildEntity);
                }
            }
        });
    }

    private static void onChannelLockDue(ChannelLockEntity channelLock, GuildEntity guildEntity) {
        if (channelLock.getUntil() == null) {
            return;
        }

        GuildMessageChannel channel = ShardManager.getLocalGuildById(channelLock.getGuildId())
                .map(guild -> guild.getChannelById(GuildMessageChannel.class, channelLock.getChannelId()))
                .orElse(null);
        if (channel == null || !PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), LockCommand.class, channel, Permission.MESSAGE_SEND, Permission.MANAGE_PERMISSIONS)) {
            return;
        }

        try {
            guildEntity.beginTransaction();
            ChannelLock.unlock(guildEntity, channel);
            guildEntity.commitTransaction();
        } catch (Throwable e) {
            guildEntity.getEntityManager().getTransaction().rollback();
            throw e;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(Command.getCommandProperties(UnlockCommand.class).emoji() + " " + Command.getCommandLanguage(UnlockCommand.class, guildEntity.getLocale()).getTitle())
                .setDescription(TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "unlock_success", new AtomicGuildMessageChannel(channel).getPrefixedNameInField(guildEntity.getLocale())));
        GuildMessageChannel logChannel = channel.getGuild().getChannelById(GuildMessageChannel.class, channelLock.getLogChannelId());
        if (logChannel != null && BotPermissionUtil.canWriteEmbed(logChannel)) {
            logChannel.sendMessageEmbeds(eb.build()).queue();
        }
    }

}
