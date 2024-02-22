package events.sync.events;

import commands.runnables.moderationcategory.ModSettingsCommand;
import core.PermissionCheckRuntime;
import core.ShardManager;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.BanAppealEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.UserSnowflake;
import org.json.JSONObject;

@SyncServerEvent(event = "BAN_APPEAL_INIT")
public class OnBanAppealInit implements SyncServerFunction {

    public enum Response {
        OK, GUILD_NOT_FOUND, NOT_CONFIGURED, MISSING_PERMISSIONS, NOT_BANNED, APPEAL_OPEN, APPEAL_DECLINED
    }

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long userId = jsonObject.getLong("user_id");
        long guildId = jsonObject.getLong("guild_id");

        Guild guild = ShardManager.getLocalGuildById(guildId).orElse(null);
        Response response = getResponse(guild, userId);

        JSONObject responseJson = new JSONObject();
        responseJson.put("response", response.name());
        if (guild != null) {
            responseJson.put("guild_name", guild.getName());
            responseJson.put("guild_icon", guild.getIconUrl());
        }
        return responseJson;
    }

    public static Response getResponse(Guild guild, long userId) {
        if (guild == null) {
            return Response.GUILD_NOT_FOUND;
        }

        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guild.getIdLong(), OnBanAppealInit.class)) {
            if (guildEntity.getModeration().getBanAppealLogChannelIdEffectively() == null) {
                return Response.NOT_CONFIGURED;
            }
            if (!PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), ModSettingsCommand.class, guild, Permission.BAN_MEMBERS)) {
                return Response.MISSING_PERMISSIONS;
            }

            try {
                guild.retrieveBan(UserSnowflake.fromId(userId)).complete();
            } catch (Throwable e) {
                return Response.NOT_BANNED;
            }

            BanAppealEntity banAppealEntity = guildEntity.getModeration().getBanAppeals().get(userId);
            if (banAppealEntity != null) {
                return banAppealEntity.getOpen() ? Response.APPEAL_OPEN : Response.APPEAL_DECLINED;
            }
        }

        return Response.OK;
    }

}
