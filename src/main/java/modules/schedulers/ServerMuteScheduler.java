package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.MuteCommand;
import core.*;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import modules.Mod;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ServerMuteScheduler {

    public static void start() {
        try {
            DBServerMute.getInstance().retrieveAll()
                    .forEach(ServerMuteScheduler::loadServerMute);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start server mute", e);
        }
    }

    public static void loadServerMute(ServerMuteData serverMuteData) {
        serverMuteData.getExpirationTime()
                .ifPresent(expirationTime -> loadServerMute(serverMuteData.getGuildId(), serverMuteData.getMemberId(), expirationTime));
    }

    public static void loadServerMute(long guildId, long memberId, Instant expires) {
        MainScheduler.schedule(expires, () -> {
            CustomObservableMap<Long, ServerMuteData> map = DBServerMute.getInstance().retrieve(guildId);
            if (map.containsKey(memberId) &&
                    map.get(memberId).getExpirationTime().orElse(Instant.MIN).getEpochSecond() == expires.getEpochSecond() &&
                    ShardManager.guildIsManaged(guildId) &&
                    ShardManager.getLocalGuildById(guildId).isPresent()
            ) {
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId, ServerMuteScheduler.class)) {
                    onServerMuteExpire(guildEntity, map.get(memberId));
                }
            }
        });
    }

    private static void onServerMuteExpire(GuildEntity guildEntity, ServerMuteData serverMuteData) {
        DBServerMute.getInstance().retrieve(serverMuteData.getGuildId())
                .remove(serverMuteData.getMemberId(), serverMuteData);

        Member member = MemberCacheController.getInstance().loadMember(serverMuteData.getGuild().get(), serverMuteData.getMemberId()).join();
        String prefix = guildEntity.getPrefix();
        Locale locale = guildEntity.getLocale();

        if (member != null) {
            Command command = CommandManager.createCommandByClass(MuteCommand.class, locale, prefix);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "mute_expired", StringUtil.escapeMarkdown(member.getUser().getAsTag())));
            Mod.postLogMembers(command, eb, guildEntity.getModeration(), member);
        } else {
            User user;
            try {
                user = ShardManager.fetchUserById(serverMuteData.getMemberId()).get();
            } catch (InterruptedException | ExecutionException e) {
                // Ignore
                return;
            }
            Command command = CommandManager.createCommandByClass(MuteCommand.class, locale, prefix);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "mute_expired", StringUtil.escapeMarkdown(user.getAsTag())));
            Mod.postLogUsers(command, eb, serverMuteData.getGuild().get(), guildEntity.getModeration(), user);
        }
    }

}
