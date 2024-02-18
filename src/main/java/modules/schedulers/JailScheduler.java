package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.JailCommand;
import core.*;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import modules.Jail;
import modules.Mod;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.jails.DBJails;
import mysql.modules.jails.JailData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.Instant;
import java.util.Locale;

public class JailScheduler {

    public static void start() {
        try {
            DBJails.getInstance().retrieveAll()
                    .forEach(JailScheduler::loadJail);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start jail", e);
        }
    }

    public static void loadJail(JailData jailData) {
        jailData.getExpirationTime()
                .ifPresent(expirationTime -> loadJail(jailData.getGuildId(), jailData.getMemberId(), expirationTime));
    }

    public static void loadJail(long guildId, long memberId, Instant expires) {
        MainScheduler.schedule(expires, () -> {
            CustomObservableMap<Long, JailData> map = DBJails.getInstance().retrieve(guildId);
            if (map.containsKey(memberId) &&
                    map.get(memberId).getExpirationTime().orElse(Instant.MIN).getEpochSecond() == expires.getEpochSecond() &&
                    ShardManager.guildIsManaged(guildId) &&
                    ShardManager.getLocalGuildById(guildId).isPresent()
            ) {
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId, JailScheduler.class)) {
                    onJailExpire(guildEntity, map.get(memberId));
                }
            }
        });
    }

    private static void onJailExpire(GuildEntity guildEntity, JailData jailData) {
        DBJails.getInstance().retrieve(jailData.getGuildId())
                .remove(jailData.getMemberId(), jailData);

        Member member = MemberCacheController.getInstance().loadMember(jailData.getGuild().get(), jailData.getMemberId()).join();
        if (member != null) {
            Locale locale = guildEntity.getLocale();
            String prefix = guildEntity.getPrefix();
            Guild guild = jailData.getGuild().get();
            Jail.unjail(jailData, guild, member, TextManager.getString(locale, Category.MODERATION, "jail_expired_title"), guildEntity);

            Command command = CommandManager.createCommandByClass(JailCommand.class, locale, prefix);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "jail_expired", StringUtil.escapeMarkdown(member.getUser().getAsTag())));
            Mod.postLogMembers(command, eb, member.getGuild(), member);
        }
    }

}
