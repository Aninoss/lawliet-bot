package modules.repair;

import commands.Command;
import commands.runnables.configurationcategory.AutoChannelCommand;
import core.PermissionCheckRuntime;
import core.ShardManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.AutoChannelEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.internal.utils.concurrent.CountingThreadFactory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoChannelRepair {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new CountingThreadFactory(() -> "Main", "AutoChannelRepair", false));

    public void start(JDA jda) {
        executorService.submit(() -> run(jda));
    }

    public void run(JDA jda) {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(AutoChannelRepair.class)) {
            entityManager.findAllWithValue(GuildEntity.class, "autoChannel.active", true).stream()
                    .filter(guildEntity -> ShardManager.getResponsibleShard(guildEntity.getGuildId()) == jda.getShardInfo().getShardId())
                    .forEach(this::deleteEmptyVoiceChannels);
        }
    }

    private void deleteEmptyVoiceChannels(GuildEntity guildEntity) {
        Guild guild = ShardManager.getLocalGuildById(guildEntity.getGuildId()).orElse(null);
        if (guild == null) {
            return;
        }

        AutoChannelEntity autoChannelEntity = guildEntity.getAutoChannel();
        Locale locale = guildEntity.getLocale();
        for (long channelId : new ArrayList<>(autoChannelEntity.getChildChannelIdsToParentChannelId().keySet())) {
            VoiceChannel voiceChannel = guild.getVoiceChannelById(channelId);
            if (voiceChannel == null) {
                autoChannelEntity.beginTransaction();
                autoChannelEntity.getChildChannelIdsToParentChannelId().remove(channelId);
                autoChannelEntity.commitTransaction();
                continue;
            }

            if (voiceChannel.getMembers().isEmpty() && PermissionCheckRuntime.botHasPermission(locale, AutoChannelCommand.class, voiceChannel, Permission.MANAGE_CHANNEL, Permission.VOICE_CONNECT)) {
                voiceChannel.delete()
                        .reason(Command.getCommandLanguage(AutoChannelCommand.class, locale).getTitle())
                        .queue();
            }
        }
    }

}
