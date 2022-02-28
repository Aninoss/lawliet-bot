package websockets.syncserver.events;

import core.MemberCacheController;
import core.ShardManager;
import core.utils.BotPermissionUtil;
import dashboard.component.DashboardComboBox;
import net.dv8tion.jda.api.entities.Member;
import org.json.JSONArray;
import org.json.JSONObject;
import websockets.syncserver.SyncServerEvent;
import websockets.syncserver.SyncServerFunction;

@SyncServerEvent(event = "DASH_LIST_DISCORD_ENTITIES")
public class OnDashboardListDiscordEntities implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        JSONObject resultJson = new JSONObject();
        JSONArray entitiesJson = new JSONArray();

        long userId = jsonObject.getLong("user_id");
        long guildId = jsonObject.getLong("guild_id");
        DashboardComboBox.DataType type = DashboardComboBox.DataType.valueOf(jsonObject.getString("type"));
        String filterText = jsonObject.getString("filter_text").toLowerCase();
        int offset = jsonObject.getInt("offset");
        int limit = jsonObject.getInt("limit");

        ShardManager.getLocalGuildById(guildId).ifPresent(guild -> {
            switch (type) {
                case MEMBERS -> MemberCacheController.getInstance().loadMembersFull(guild).join().stream()
                        .filter(m -> m.getUser().getAsTag().toLowerCase().contains(filterText))
                        .skip(offset)
                        .limit(limit)
                        .forEach(m -> {
                            JSONObject json = new JSONObject();
                            json.put("id", m.getId());
                            json.put("name", m.getUser().getAsTag());
                            entitiesJson.put(json);
                        });

                case ROLES -> guild.getRoles().stream()
                        .filter(r -> r.getName().toLowerCase().contains(filterText) && !r.isPublicRole() && !r.isManaged())
                        .skip(offset)
                        .limit(limit)
                        .forEach(r -> {
                            JSONObject json = new JSONObject();
                            json.put("id", r.getId());
                            json.put("name", r.getName());
                            entitiesJson.put(json);
                        });

                case TEXT_CHANNELS -> {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                    guild.getTextChannels().stream()
                            .filter(c -> ("#" + c.getName().toLowerCase()).contains(filterText) && BotPermissionUtil.can(member, c))
                            .skip(offset)
                            .limit(limit)
                            .forEach(c -> {
                                JSONObject json = new JSONObject();
                                json.put("id", c.getId());
                                json.put("name", "#" + c.getName());
                                entitiesJson.put(json);
                            });
                }

                case VOICE_CHANNELS -> {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                    guild.getVoiceChannels().stream()
                            .filter(c -> c.getName().toLowerCase().contains(filterText) && BotPermissionUtil.can(member, c))
                            .skip(offset)
                            .limit(limit)
                            .forEach(c -> {
                                JSONObject json = new JSONObject();
                                json.put("id", c.getId());
                                json.put("name", c.getName());
                                entitiesJson.put(json);
                            });
                }
            }
        });

        resultJson.put("entities", entitiesJson);
        return resultJson;
    }

}
