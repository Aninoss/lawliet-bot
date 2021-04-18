package modules.schedulers;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.BanCommand;
import constants.Category;
import core.*;
import core.schedule.MainScheduler;
import modules.Mod;
import mysql.modules.tempban.DBTempBan;
import mysql.modules.tempban.TempBanData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

public class TempBanScheduler {

    private static final TempBanScheduler ourInstance = new TempBanScheduler();

    public static TempBanScheduler getInstance() {
        return ourInstance;
    }

    private TempBanScheduler() {
    }

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBTempBan.getInstance().retrieveAll()
                    .forEach(this::loadTempBan);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start temp ban", e);
        }
    }

    public void loadTempBan(TempBanData tempBanData) {
        loadTempBan(tempBanData.getGuildId(), tempBanData.getMemberId(), tempBanData.getExpirationTime());
    }

    public void loadTempBan(long guildId, long memberId, Instant expires) {
        MainScheduler.getInstance().schedule(expires, "tempban_" + guildId, () -> {
            CustomObservableMap<Long, TempBanData> map = DBTempBan.getInstance().retrieve(guildId);
            if (map.containsKey(memberId) &&
                    map.get(memberId).getExpirationTime().getEpochSecond() == expires.getEpochSecond() &&
                    ShardManager.getInstance().guildIsManaged(guildId)
            ) {
                onTempBanExpire(map.get(memberId));
            }
        });
    }

    private void onTempBanExpire(TempBanData tempBanData) {
        DBTempBan.getInstance().retrieve(tempBanData.getGuildId())
                .remove(tempBanData.getMemberId(), tempBanData);

        Locale locale = tempBanData.getGuildBean().getLocale();
        tempBanData.getGuild()
                .ifPresent(guild -> {
                    if (PermissionCheckRuntime.getInstance().botHasPermission(
                            locale,
                            BanCommand.class,
                            guild,
                            Permission.BAN_MEMBERS
                    ) && guild.getMemberById(tempBanData.getMemberId()) == null) {
                        guild.unban(String.valueOf(tempBanData.getMemberId()))
                                .reason(TextManager.getString(locale, Category.MODERATION, "ban_expired_title"))
                                .queue();

                        ShardManager.getInstance().fetchUserById(tempBanData.getMemberId()).thenAccept(user -> {
                            Command command = CommandManager.createCommandByClass(BanCommand.class, locale, tempBanData.getGuildBean().getPrefix());
                            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "ban_expired", user.getAsTag()));
                            Mod.postLogUsers(command, eb, guild, user);
                        });
                    }
                });
    }

}
