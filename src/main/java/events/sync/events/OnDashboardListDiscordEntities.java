package events.sync.events;

import core.MemberCacheController;
import core.ShardManager;
import core.atomicassets.AtomicGuildChannel;
import core.atomicassets.AtomicGuildMessageChannel;
import core.atomicassets.AtomicStandardGuildMessageChannel;
import core.emoji.EmojiTable;
import core.utils.BotPermissionUtil;
import dashboard.component.DashboardComboBox;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.stream.Collectors;

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
                        .filter(m -> m.getUser().getName().toLowerCase().contains(filterText))
                        .skip(offset)
                        .limit(limit)
                        .forEach(m -> {
                            JSONObject json = new JSONObject();
                            json.put("id", m.getId());
                            json.put("name", m.getUser().getName());
                            entitiesJson.put(json);
                        });

                case ROLES -> guild.getRoles().stream()
                        .filter(r -> r.getName().toLowerCase().contains(filterText) && !r.isPublicRole())
                        .skip(offset)
                        .limit(limit)
                        .forEach(r -> {
                            JSONObject json = new JSONObject();
                            json.put("id", r.getId());
                            json.put("name", r.getName());
                            entitiesJson.put(json);
                        });

                case GUILD_CHANNELS -> {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                    guild.getChannelCache().stream()
                            .filter(c -> new AtomicGuildChannel(c).getPrefixedNameRaw().orElse("").toLowerCase().contains(filterText) &&
                                    BotPermissionUtil.can(member, c)
                            )
                            .sorted()
                            .skip(offset)
                            .limit(limit)
                            .forEach(c -> {
                                JSONObject json = new JSONObject();
                                json.put("id", c.getId());
                                json.put("name", new AtomicGuildChannel(c).getPrefixedNameRaw().orElse(""));
                                entitiesJson.put(json);
                            });
                }

                case GUILD_MESSAGE_CHANNELS -> {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                    guild.getChannelCache().stream()
                            .filter(c -> c instanceof GuildMessageChannel &&
                                    new AtomicGuildMessageChannel((GuildMessageChannel) c).getPrefixedNameRaw().orElse("").toLowerCase().contains(filterText) &&
                                    BotPermissionUtil.can(member, c)
                            )
                            .map(c -> (GuildMessageChannel) c)
                            .sorted()
                            .skip(offset)
                            .limit(limit)
                            .forEach(c -> {
                                JSONObject json = new JSONObject();
                                json.put("id", c.getId());
                                json.put("name", new AtomicGuildMessageChannel(c).getPrefixedNameRaw().orElse(""));
                                entitiesJson.put(json);
                            });
                }

                case STANDARD_GUILD_MESSAGE_CHANNELS -> {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).join();
                    guild.getChannelCache().stream()
                            .filter(c -> c instanceof StandardGuildMessageChannel &&
                                    new AtomicStandardGuildMessageChannel((StandardGuildMessageChannel) c).getPrefixedNameRaw().orElse("").toLowerCase().contains(filterText) &&
                                    BotPermissionUtil.can(member, c)
                            )
                            .map(c -> (StandardGuildMessageChannel) c)
                            .sorted()
                            .skip(offset)
                            .limit(limit)
                            .forEach(c -> {
                                JSONObject json = new JSONObject();
                                json.put("id", c.getId());
                                json.put("name", new AtomicStandardGuildMessageChannel(c).getPrefixedNameRaw().orElse(""));
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
                                json.put("name", "🔊" + c.getName());
                                entitiesJson.put(json);
                            });
                }

                case EMOJI -> {
                    List<RichCustomEmoji> matchedEmojis = guild.getEmojis().stream()
                            .filter(emoji -> emoji.getName().toLowerCase().contains(filterText))
                            .collect(Collectors.toList());

                    matchedEmojis.stream()
                            .skip(offset)
                            .limit(limit)
                            .forEach(emoji -> {
                                JSONObject json = new JSONObject();
                                json.put("id", emoji.getFormatted());
                                json.put("name", emoji.getName());
                                json.put("icon_url", emoji.getImageUrl());
                                entitiesJson.put(json);
                            });
                    EmojiTable.getEmojis().stream()
                            .filter(pair -> pair.getKey().toLowerCase().contains(filterText) || pair.getValue().equals(filterText))
                            .skip(Math.max(0, offset - matchedEmojis.size()))
                            .limit(Math.max(0, limit - entitiesJson.length()))
                            .forEach(pair -> {
                                JSONObject json = new JSONObject();
                                json.put("id", pair.getValue());
                                json.put("name", pair.getKey());
                                json.put("icon_url", pair.getValue());
                                entitiesJson.put(json);
                            });
                }
            }
        });

        resultJson.put("entities", entitiesJson);
        return resultJson;
    }

}
