package events.sync;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import core.PatreonData;
import core.Program;
import core.cache.PatreonCache;
import core.restclient.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class SendEvent {

    private SendEvent() {
    }

    public static CompletableFuture<Boolean> sendPing() {
        return sendEmpty("PING")
                .thenApply(responseJson -> responseJson.has("ping") && responseJson.getString("ping").equals("pong"));
    }

    public static CompletableFuture<Optional<Long>> sendRequestGlobalGuildSize(long localServerSize) {
        return process(
                "GLOBAL_SERVER_SIZE",
                Map.of("local_server_size", localServerSize),
                responseJson -> {
                    long size = responseJson.getLong("size");
                    return size > 0 ? Optional.of(size) : Optional.empty();
                }
        );
    }

    public static CompletableFuture<Optional<String>> sendRequestCustomEmoji(long emojiId) {
        return process(
                "CUSTOM_EMOJI",
                Map.of("emoji_id", emojiId),
                responseJson -> responseJson.has("tag") ? Optional.of(responseJson.getString("tag")) : Optional.empty()
        );
    }

    public static CompletableFuture<Optional<String>> sendRequestServerName(long serverId) {
        return process(
                "SERVER_NAME",
                Map.of("server_id", serverId),
                responseJson -> responseJson.has("name") ? Optional.of(responseJson.getString("name")) : Optional.empty()
        );
    }

    public static CompletableFuture<PatreonData> sendRequestPatreon() {
        return process(
                "PATREON",
                Map.of(),
                PatreonCache::patreonDataFromJson
        );
    }

    public static CompletableFuture<Void> sendUnreport(String url) {
        return process(
                "UNREPORT",
                Map.of("url", url),
                responseJson -> null
        );
    }

    public static CompletableFuture<Void> sendHeartbeat(String ip, boolean alreadyConnected, int totalShards, long totalServers) {
        return sendHeartbeat(ip, alreadyConnected, totalShards, totalServers, null);
    }

    public static CompletableFuture<Void> sendHeartbeat(String ip, boolean alreadyConnected, int totalShards, long totalServers,
                                                        Collection<Long> serverIds) {
        JSONObject requestJson = new JSONObject();
        requestJson.put("ip", ip);
        requestJson.put("already_connected", alreadyConnected);
        requestJson.put("total_shards", totalShards);
        requestJson.put("total_servers", totalServers);

        if (serverIds != null) {
            JSONArray serverIdsJsonArray = new JSONArray();
            for (long serverId : serverIds) {
                serverIdsJsonArray.put(serverId);
            }
            requestJson.put("server_ids", serverIdsJsonArray);
        }

        return process(
                "HEARTBEAT",
                requestJson,
                responseJson -> null
        );
    }

    public static CompletableFuture<Void> sendFeatureRequestAction(int id, boolean accept, String reason) {
        return process(
                "FR_ACTION",
                Map.of(
                        "id", id,
                        "accept", accept,
                        "reason", reason
                ),
                responseJson -> null
        );
    }

    public static CompletableFuture<JSONObject> sendEmpty(String event) {
        return process(event, Collections.emptyMap(), r -> r);
    }

    private static <T> CompletableFuture<T> process(String event, Map<String, Object> jsonMap, Function<JSONObject, T> function) {
        JSONObject dataJson = new JSONObject();
        jsonMap.keySet().forEach(k -> dataJson.put(k, jsonMap.get(k)));
        return process(event, dataJson, function);
    }

    private static <T> CompletableFuture<T> process(String event, JSONObject dataJson, Function<JSONObject, T> function) {
        dataJson.put("source_cluster_id", Program.getClusterId());
        return RestClient.SYNC.post(event, "application/json", dataJson.toString())
                .thenApply(jsonResponse -> function.apply(new JSONObject(jsonResponse.getBody())));
    }

}
