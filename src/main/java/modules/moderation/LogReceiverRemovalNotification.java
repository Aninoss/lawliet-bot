package modules.moderation;

import commands.Category;
import core.EmbedFactory;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class LogReceiverRemovalNotification {

    private static final long DELAY_MILLIS = 2000;

    public static void execute() throws InterruptedException {
        if (Program.getClusterId() != 1) {
            MainLogger.get().error("Log receiver removal notifications must be sent from the main cluster");
            return;
        }

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(LogReceiverRemovalNotification.class)) {
            String queryString = """
                    {
                      $or: [
                        { "inviteFilter.logReceiverUserIds.0": { $exists: true } },
                        { "spamFilter.logReceiverUserIds.0": { $exists: true } },
                        { "wordFilter.logReceiverUserIds.0": { $exists: true } }
                      ]
                    }
                    """;

            List<GuildEntity> guildEntityList = entityManager.createNativeQuery(queryString, GuildEntity.class).getResultList();
            JDA jda = ShardManager.getAnyJDA().get();
            long selfUserId = jda.getSelfUser().getIdLong();

            int sentCounter = 0;
            for (GuildEntity guildEntity : guildEntityList) {
                guildEntity.setEntityManager(entityManager);
                long guildId = guildEntity.getGuildId();

                Set<Long> userIds = new LinkedHashSet<>();
                userIds.addAll(guildEntity.getInviteFilter().getLogReceiverUserIds());
                userIds.addAll(guildEntity.getSpamFilter().getLogReceiverUserIds());
                userIds.addAll(guildEntity.getWordFilter().getLogReceiverUserIds());
                userIds.remove(selfUserId);
                if (userIds.isEmpty()) {
                    continue;
                }

                Optional<String> guildNameOpt = ShardManager.getGuildName(guildId);
                if (guildNameOpt.isEmpty()) {
                    MainLogger.get().warn("Skipping log receiver removal notifications for guild {}: name not found", guildId);
                    continue;
                }

                for (long userId : userIds) {
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "logreceiverremoval_title"))
                            .setDescription(TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "logreceiverremoval_description",
                                    StringUtil.escapeMarkdown(guildNameOpt.get()), guildEntity.getPrefix()));

                    try {
                        JDAUtil.openPrivateChannel(jda, userId)
                                .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                                .complete();
                        sentCounter++;
                    } catch (Throwable e) {
                        MainLogger.get().info("Could not send log receiver removal notification to user {} in guild {}", userId, guildId);
                    }

                    removeLogReceiver(guildEntity, userId);
                    Thread.sleep(DELAY_MILLIS);
                }
            }

            MainLogger.get().info("{} log receiver removal notifications sent", sentCounter);
        }
    }

    private static void removeLogReceiver(GuildEntity guildEntity, long userId) {
        guildEntity.beginTransaction();
        guildEntity.getInviteFilter().getLogReceiverUserIds().removeIf(id -> id == userId);
        guildEntity.getSpamFilter().getLogReceiverUserIds().removeIf(id -> id == userId);
        guildEntity.getWordFilter().getLogReceiverUserIds().removeIf(id -> id == userId);
        guildEntity.commitTransaction();
    }

}
