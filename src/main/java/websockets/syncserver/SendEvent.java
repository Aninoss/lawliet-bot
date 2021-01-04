package websockets.syncserver;

import core.Bot;
import core.DiscordApiManager;
import core.utils.DiscordUtil;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class SendEvent {

    private SendEvent() {}

    public static CompletableFuture<JSONObject> sendFullyConnected() {
        CompletableFuture<JSONObject> future = SyncManager.getInstance().getClient().send("CLUSTER_FULLY_CONNECTED", new JSONObject());
        if (Bot.isProductionMode())
            return future;
        else
            return CompletableFuture.completedFuture(new JSONObject());
    }

    public static CompletableFuture<Optional<Long>> sendRequestGlobalServerSize(long localServerSize) {
        if (!Bot.isProductionMode())
            return CompletableFuture.completedFuture(DiscordApiManager.getInstance().getLocalServerSize());

        return process("GLOBAL_SERVER_SIZE",
                Map.of("local_server_size", localServerSize),
                responseJson -> {
                    long size = responseJson.getLong("size");
                    return size > 0 ? Optional.of(size) : Optional.empty();
                }
        );
    }

    public static CompletableFuture<Optional<CustomEmoji>> sendRequestCustomEmoji(long emojiId) {
        return process("CUSTOM_EMOJI",
                Map.of("emoji_id", emojiId),
                responseJson -> {
                    Optional<String> tag = responseJson.has("tag") ? Optional.of(responseJson.getString("tag")) : Optional.empty();
                    return tag.map(DiscordUtil::createCustomEmojiFromTag);
                }
        );
    }

    public static CompletableFuture<Optional<String>> sendRequestServerName(long serverId) {
        return process("SERVER_NAME",
                Map.of("server_id", serverId),
                responseJson -> responseJson.has("name") ? Optional.of(responseJson.getString("name")) : Optional.empty()
        );
    }

    public static CompletableFuture<HashMap<Long, Integer>> sendRequestPatreon() {
        return process("PATREON",
                Map.of(),
                responseJson -> {
                    HashMap<Long, Integer> patreonUserTiers = new HashMap<>();
                    JSONArray usersArray = responseJson.getJSONArray("users");
                    for (int i = 0; i < usersArray.length(); i++) {
                        JSONObject userJson = usersArray.getJSONObject(i);
                        patreonUserTiers.put(
                                userJson.getLong("user_id"),
                                userJson.getInt("tier")
                        );
                    }
                    return patreonUserTiers;
                }
        );
    }

    public static CompletableFuture<Long> sendRequestSyncedRatelimit() {
        return process("SYNCED_RATELIMIT",
                Map.of(),
                responseJson -> responseJson.getLong("waiting_time_nanos")
        );
    }

    private static <T> CompletableFuture<T> process(String event, Map<String, Object> jsonMap, Function<JSONObject, T> function) {
        CompletableFuture<T> future = new CompletableFuture<>();

        JSONObject dataJson = new JSONObject();
        jsonMap.keySet().forEach(k -> dataJson.put(k, jsonMap.get(k)));
        SyncManager.getInstance().getClient().send(event, dataJson)
                .exceptionally(e -> {
                    future.completeExceptionally(e);
                    return null;
                })
                .thenAccept(jsonResponse -> {
                    T t = function.apply(jsonResponse);
                    future.complete(t);
                });

        return future;
    }

}
