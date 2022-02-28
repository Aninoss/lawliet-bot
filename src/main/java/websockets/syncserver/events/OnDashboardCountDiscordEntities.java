package websockets.syncserver.events;

import core.MemberCacheController;
import core.ShardManager;
import core.utils.BotPermissionUtil;
import dashboard.component.DashboardComboBox;
import net.dv8tion.jda.api.entities.Member;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "DASH_COUNT_DISCORD_ENTITIES")
public class OnDashboardCountDiscordEntities implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject resultJson = new JSONObject();

        long userId = jsonObject.getLong("user_id");
        long guildId = jsonObject.getLong("guild_id");
        DashboardComboBox.DataType type = DashboardComboBox.DataType.valueOf(jsonObject.getString("type"));
        String filterText = jsonObject.getString("filter_text").toLowerCase();

        long count = ShardManager.getLocalGuildById(guildId).map(guild -> switch (type) {
            case MEMBERS -> MemberCacheController.getInstance().loadMembersFull(guild).join().stream()
                    .filter(m -> m.getUser().getAsTag().toLowerCase().contains(filterText))
                    .count();

            case ROLES -> guild.getRoles().stream()
                    .filter(r -> r.getName().toLowerCase().contains(filterText) && !r.isPublicRole() && !r.isManaged())
                    .count();

            case TEXT_CHANNELS -> {
                Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                yield guild.getTextChannels().stream()
                        .filter(c -> ("#" + c.getName().toLowerCase()).contains(filterText) && BotPermissionUtil.can(member, c))
                        .count();
            }

            case VOICE_CHANNELS -> {
                Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                yield guild.getVoiceChannels().stream()
                        .filter(c -> c.getName().toLowerCase().contains(filterText) && BotPermissionUtil.can(member, c))
                        .count();
            }

            default -> 0L;
        }).orElse(0L);

        resultJson.put("count", count);
        return resultJson;
    }

}
