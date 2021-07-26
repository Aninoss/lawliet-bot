package modules.porn;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import constants.Settings;
import core.restclient.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class BooruImageDownloader {

    public CompletableFuture<Optional<BooruImage>> getPicture(long guildId, String domain, String searchTerm,
                                                              boolean animatedOnly, boolean explicit,
                                                              Set<String> filters, List<String> skippedResults
    ) throws ExecutionException {
        JSONArray filtersJson = new JSONArray();
        filters = new HashSet<>(filters);
        filters.addAll(Arrays.asList(Settings.NSFW_FILTERS));
        filters.forEach(filtersJson::put);

        JSONArray skippedResultsJson = new JSONArray();
        skippedResults.forEach(skippedResultsJson::put);

        JSONObject json = new JSONObject();
        json.put("guildId", guildId);
        json.put("domain", domain);
        json.put("searchTerm", searchTerm);
        json.put("searchTermExtra", ""); //TODO: remove
        json.put("animatedOnly", animatedOnly);
        json.put("explicit", explicit);
        json.put("filters", filtersJson);
        json.put("skippedResults", skippedResultsJson);

        return RestClient.WEBCACHE.post("booru", "application/json", json.toString())
                .thenApply(response -> {
                    String content = response.getBody();
                    if (content.length() > 0 && content.startsWith("{")) {
                        JSONObject responseJson = new JSONObject(content);
                        BooruImage booruImage = new BooruImage()
                                .setScore(responseJson.getInt("score"))
                                .setImageUrl(responseJson.getString("imageUrl"))
                                .setPageUrl(responseJson.getString("pageUrl"))
                                .setVideo(responseJson.getBoolean("video"))
                                .setInstant(Instant.parse(responseJson.getString("instant")));

                        return Optional.of(booruImage);
                    } else {
                        return Optional.empty();
                    }
                });
    }

}
