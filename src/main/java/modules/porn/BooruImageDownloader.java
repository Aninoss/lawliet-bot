package modules.porn;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import constants.Settings;
import core.restclient.RestClient;
import org.json.JSONArray;
import org.json.JSONObject;

public class BooruImageDownloader {

    public CompletableFuture<Optional<BooruImage>> getPicture(long guildId, String domain, String searchTerm,
                                                              String searchTermExtra, boolean animatedOnly,
                                                              boolean explicit, List<String> filters,
                                                              List<String> skippedResults
    ) throws ExecutionException {
        JSONArray filtersJson = new JSONArray();
        Arrays.asList(Settings.NSFW_FILTERS).forEach(filtersJson::put);
        filters.forEach(filtersJson::put);

        JSONArray skippedResultsJson = new JSONArray();
        skippedResults.forEach(skippedResultsJson::put);

        JSONObject json = new JSONObject();
        json.put("guildId", guildId);
        json.put("domain", domain);
        json.put("searchTerm", searchTerm);
        json.put("searchTermExtra", searchTermExtra);
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
