package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.BanCommand;
import core.*;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import modules.Mod;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.UserSnowflake;

import java.time.Instant;
import java.util.Locale;

public class TempBanScheduler {

    public static void start() {
        try {
            DBTempBan.getInstance().retrieveAll()
                    .forEach(TempBanScheduler::loadTempBan);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start temp ban", e);
        }
    }

    public static void loadTempBan(TempBanData tempBanData) {
        loadTempBan(tempBanData.getGuildId(), tempBanData.getMemberId(), tempBanData.getExpirationTime());
    }

    public static void loadTempBan(long guildId, long memberId, Instant expires) {
        MainScheduler.schedule(expires, "tempban_" + guildId, () -> {
            CustomObservableMap<Long, TempBanData> map = DBTempBan.getInstance().retrieve(guildId);
            if (map.containsKey(memberId) &&
                    map.get(memberId).getExpirationTime().getEpochSecond() == expires.getEpochSecond() &&
                    ShardManager.guildIsManaged(guildId) &&
                    ShardManager.getLocalGuildById(guildId).isPresent()
            ) {
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId)) {
                    onTempBanExpire(guildEntity, map.get(memberId));
                }
            }
        });
    }

    private static void onTempBanExpire(GuildEntity guildEntity, TempBanData tempBanData) {
        DBTempBan.getInstance().retrieve(tempBanData.getGuildId())
                .remove(tempBanData.getMemberId(), tempBanData);

        tempBanData.getGuild()
                .ifPresent(guild -> {
                    String prefix = guildEntity.getPrefix();
                    Locale locale = guildEntity.getLocale();

                    if (PermissionCheckRuntime.botHasPermission(
                            locale,
                            BanCommand.class,
                            guild,
                            Permission.BAN_MEMBERS
                    )) {
                        guild.unban(UserSnowflake.fromId(tempBanData.getMemberId()))
                                .reason(TextManager.getString(locale, Category.MODERATION, "ban_expired_title"))
                                .queue();

                        ShardManager.fetchUserById(tempBanData.getMemberId()).thenAccept(user -> {
                            Command command = CommandManager.createCommandByClass(BanCommand.class, locale, prefix);
                            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "ban_expired", StringUtil.escapeMarkdown(user.getAsTag())));
                            Mod.postLogUsers(command, eb, guild, user);
                        });
                    }
                });
    }

}
