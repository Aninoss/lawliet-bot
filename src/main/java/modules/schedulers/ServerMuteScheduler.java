package modules.schedulers;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.MuteCommand;
import commands.Category;
import core.*;
import core.schedule.MainScheduler;
import modules.Mod;
import mysql.modules.moderation.DBModeration;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;

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
        MainScheduler.schedule(expires, "servermute_" + guildId, () -> {
            CustomObservableMap<Long, ServerMuteData> map = DBServerMute.getInstance().retrieve(guildId);
            if (map.containsKey(memberId) &&
                    map.get(memberId).getExpirationTime().orElse(Instant.MIN).getEpochSecond() == expires.getEpochSecond() &&
                    ShardManager.guildIsManaged(guildId)
            ) {
                onServerMuteExpire(map.get(memberId));
            }
        });
    }

    private static void onServerMuteExpire(ServerMuteData serverMuteData) {
        Locale locale = serverMuteData.getGuildData().getLocale();
        DBServerMute.getInstance().retrieve(serverMuteData.getGuildId())
                .remove(serverMuteData.getMemberId(), serverMuteData);

        MemberCacheController.getInstance().loadMember(serverMuteData.getGuild().get(), serverMuteData.getMemberId()).thenAccept(member -> {
            if (member != null) {
                if (serverMuteData.isNewMethod()) {
                    Role muteRole = DBModeration.getInstance().retrieve(member.getGuild().getIdLong()).getMuteRole().orElse(null);
                    if (muteRole != null && PermissionCheckRuntime.botCanManageRoles(locale, MuteCommand.class, muteRole)) {
                        member.getGuild().removeRoleFromMember(member, muteRole)
                                .reason(TextManager.getString(locale, Category.MODERATION, "mute_expired_title"))
                                .queue();
                    }
                } else {
                    Role muteRole = DBModeration.getInstance().retrieve(member.getGuild().getIdLong()).getMuteRole().orElse(null);
                    if (muteRole != null && PermissionCheckRuntime.botCanManageRoles(locale, MuteCommand.class, muteRole)) {
                        member.getGuild().removeRoleFromMember(member, muteRole)
                                .reason(TextManager.getString(locale, Category.MODERATION, "mute_expired_title"))
                                .queue();
                    }
                }

                Command command = CommandManager.createCommandByClass(MuteCommand.class, locale, serverMuteData.getGuildData().getPrefix());
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "mute_expired", member.getUser().getAsTag()));
                Mod.postLogMembers(command, eb, member.getGuild(), member);
            } else {
                ShardManager.fetchUserById(serverMuteData.getMemberId())
                        .thenAccept(user -> {
                            Command command = CommandManager.createCommandByClass(MuteCommand.class, locale, serverMuteData.getGuildData().getPrefix());
                            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "mute_expired", user.getAsTag()));
                            Mod.postLogUsers(command, eb, serverMuteData.getGuild().get(), user);
                        });
            }
        });
    }

}
