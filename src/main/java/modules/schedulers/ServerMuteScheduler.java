package modules.schedulers;

import java.time.Instant;
import java.util.Locale;
import commands.Command;
import commands.CommandManager;
import commands.runnables.moderationcategory.MuteCommand;
import constants.Category;
import core.*;
import core.schedule.MainScheduler;
import modules.Mod;
import mysql.modules.moderation.DBModeration;
import mysql.modules.servermute.DBServerMute;
import mysql.modules.servermute.ServerMuteSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

public class ServerMuteScheduler {

    private static final ServerMuteScheduler ourInstance = new ServerMuteScheduler();

    public static ServerMuteScheduler getInstance() {
        return ourInstance;
    }

    private ServerMuteScheduler() {
    }

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBServerMute.getInstance().retrieveAll()
                    .forEach(this::loadServerMute);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start server mute", e);
        }
    }

    public void loadServerMute(ServerMuteSlot serverMuteSlot) {
        serverMuteSlot.getExpirationTime()
                .ifPresent(expirationTime -> loadServerMute(serverMuteSlot.getGuildId(), serverMuteSlot.getMemberId(), expirationTime));
    }

    public void loadServerMute(long guildId, long memberId, Instant expires) {
        MainScheduler.getInstance().schedule(expires, "servermute_" + guildId, () -> {
            CustomObservableMap<Long, ServerMuteSlot> map = DBServerMute.getInstance().retrieve(guildId);
            if (map.containsKey(memberId) &&
                    map.get(memberId).getExpirationTime().orElse(Instant.now()).getEpochSecond() == expires.getEpochSecond() &&
                    ShardManager.getInstance().guildIsManaged(guildId)
            ) {
                onServerMuteExpire(map.get(memberId));
            }
        });
    }

    private void onServerMuteExpire(ServerMuteSlot serverMuteSlot) {
        DBServerMute.getInstance().retrieve(serverMuteSlot.getGuildId())
                .remove(serverMuteSlot.getMemberId(), serverMuteSlot);

        serverMuteSlot.getGuild().ifPresent(guild -> {
            Locale locale = serverMuteSlot.getGuildBean().getLocale();
            Member member = serverMuteSlot.getMember().orElse(null);
            Role muteRole = DBModeration.getInstance().retrieve(guild.getIdLong()).getMuteRole().orElse(null);
            if (muteRole != null && member != null && PermissionCheckRuntime.getInstance().botCanManageRoles(locale, MuteCommand.class, muteRole)) {
                member.getGuild().removeRoleFromMember(member, muteRole)
                        .reason(TextManager.getString(locale, Category.MODERATION, "mute_expired_title"))
                        .queue();
            }

            ShardManager.getInstance().fetchUserById(serverMuteSlot.getMemberId()).thenAccept(user -> {
                Command command = CommandManager.createCommandByClass(MuteCommand.class, locale, serverMuteSlot.getGuildBean().getPrefix());
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, TextManager.getString(locale, Category.MODERATION, "mute_expired", user.getAsTag()));
                Mod.postLogUsers(command, eb, guild, user);
            });
        });
    }

}
