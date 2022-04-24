package modules.schedulers;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.BanCommand;
import commands.Category;
import core.*;
import core.schedule.MainScheduler;
import modules.Mod;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.UserSnowflake;

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
                    ShardManager.guildIsManaged(guildId)
            ) {
                onTempBanExpire(map.get(memberId));
            }
        });
    }

    private static void onTempBanExpire(TempBanData tempBanData) {
        DBTempBan.getInstance().retrieve(tempBanData.getGuildId())
                .remove(tempBanData.getMemberId(), tempBanData);

        Locale locale = tempBanData.getGuildData().getLocale();
        tempBanData.getGuild()
                .ifPresent(guild -> {
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
                            Command command = CommandManager.createCommandByClass(BanCommand.class, locale, tempBanData.getGuildData().getPrefix());
                            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "ban_expired", user.getAsTag()));
                            Mod.postLogUsers(command, eb, guild, user);
                        });
                    }
                });
    }

}
