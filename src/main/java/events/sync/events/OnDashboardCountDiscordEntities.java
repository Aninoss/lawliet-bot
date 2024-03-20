package events.sync.events;

import core.MemberCacheController;
import core.ShardManager;
import core.atomicassets.AtomicGuildChannel;
import core.atomicassets.AtomicGuildMessageChannel;
import core.atomicassets.AtomicStandardGuildMessageChannel;
import core.utils.BotPermissionUtil;
import dashboard.component.DashboardComboBox;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import org.json.JSONObject;

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
                    .filter(m -> m.getUser().getName().toLowerCase().contains(filterText))
                    .count();

            case ROLES -> guild.getRoles().stream()
                    .filter(r -> r.getName().toLowerCase().contains(filterText) && !r.isPublicRole())
                    .count();

            case GUILD_CHANNELS -> {
                Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                yield guild.getChannelCache().stream()
                        .filter(c -> new AtomicGuildChannel(c).getPrefixedNameRaw().orElse("").toLowerCase().contains(filterText) && BotPermissionUtil.can(member, c))
                        .count();
            }

            case GUILD_MESSAGE_CHANNELS -> {
                Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                yield guild.getChannelCache().stream()
                        .filter(c -> c instanceof GuildMessageChannel &&
                                new AtomicGuildMessageChannel((GuildMessageChannel) c).getPrefixedNameRaw().orElse("").toLowerCase().contains(filterText) && BotPermissionUtil.can(member, c)
                        )
                        .count();
            }

            case STANDARD_GUILD_MESSAGE_CHANNELS -> {
                Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                yield guild.getChannelCache().stream()
                        .filter(c -> c instanceof StandardGuildMessageChannel &&
                                new AtomicStandardGuildMessageChannel((StandardGuildMessageChannel) c).getPrefixedNameRaw().orElse("").toLowerCase().contains(filterText) && BotPermissionUtil.can(member, c)
                        )
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
