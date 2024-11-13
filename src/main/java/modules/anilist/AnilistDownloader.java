package modules.anilist;

import core.internet.HttpCache;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AnilistDownloader {

    private static final String SEARCH_QUERY = """
            query ($id: Int, $search: String, $isAdult: Boolean) {
                Media(id: $id, search: $search, type: ANIME, isAdult: $isAdult) {
                    id
                    title {
                        native
                        romaji
                        english
                    }
                    description
                    coverImage {
                        medium
                        large
                    }
                    siteUrl
                    status
                    isAdult
                    genres
                    episodes
                    nextAiringEpisode {
                        airingAt
                        episode
                    }
                    averageScore
                }
            }
            """;

    private static final String SUGGESTIONS_QUERY = """
            query ($search: String, $isAdult: Boolean) {
                Page {
                    media(search: $search, type: ANIME, isAdult: $isAdult) {
                        title {
                            native
                            romaji
                            english
                        }
                    }
                }
            }
            """;

    public static AnilistMedia getMediaBySearch(String search, boolean allowNsfw) throws ExecutionException, InterruptedException {
        return getMedia(search, null, allowNsfw);
    }

    public static AnilistMedia getMediaById(int id, boolean allowNsfw) throws ExecutionException, InterruptedException {
        return getMedia(null, id, allowNsfw);
    }

    public static List<String> getSuggestionsBySearch(String search, boolean allowNsfw) throws ExecutionException, InterruptedException {
        JSONObject variablesJson = new JSONObject();
        variablesJson.put("search", search.toLowerCase().trim());
        if (!allowNsfw) {
            variablesJson.put("isAdult", false);
        }

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("query", SUGGESTIONS_QUERY);
        jsonBody.put("variables", variablesJson);

        String response = HttpCache.post("https://graphql.anilist.co", jsonBody.toString(), "application/json", Duration.ofHours(24)).get()
                .getBody();
        if (response.isEmpty()) {
            return Collections.emptyList();
        }

        JSONArray mediaJson = new JSONObject(response).getJSONObject("data").getJSONObject("Page").getJSONArray("media");
        ArrayList<String> suggestions = new ArrayList<>();
        for (int i = 0; i < mediaJson.length(); i++) {
            String title = extractTitle(mediaJson.getJSONObject(i).getJSONObject("title"));
            suggestions.add(title);
        }
        return Collections.unmodifiableList(suggestions);
    }

    private static AnilistMedia getMedia(String search, Integer id, boolean allowNsfw) throws ExecutionException, InterruptedException {
        JSONObject variablesJson = new JSONObject();
        if (search != null) {
            variablesJson.put("search", search.toLowerCase().trim());
        }
        if (id != null) {
            variablesJson.put("id", id);
        }
        if (!allowNsfw) {
            variablesJson.put("isAdult", false);
        }

        JSONObject jsonBody = new JSONObject();
        jsonBody.put("query", SEARCH_QUERY);
        jsonBody.put("variables", variablesJson);

        String response = HttpCache.post("https://graphql.anilist.co", jsonBody.toString(), "application/json", Duration.ofMinutes(59)).get()
                .getBody();
        if (response.isEmpty()) {
            return null;
        }

        JSONObject dataJson = new JSONObject(response).getJSONObject("data");
        if (dataJson.isNull("Media")) {
            return null;
        }
        return extractMediaObject(dataJson.getJSONObject("Media"));
    }

    private static AnilistMedia extractMediaObject(JSONObject jsonObject) {
        JSONArray genresJson = jsonObject.getJSONArray("genres");
        ArrayList<String> genresList = new ArrayList<>();
        for (int i = 0; i < genresJson.length(); i++) {
            genresList.add(genresJson.getString(i));
        }

        return new AnilistMedia(
                jsonObject.getInt("id"),
                extractTitle(jsonObject.getJSONObject("title")),
                jsonObject.getString("description").replaceAll("<[^>]*>", ""),
                jsonObject.getJSONObject("coverImage").getString("medium"),
                jsonObject.getString("siteUrl"),
                AnilistMedia.Status.valueOf(jsonObject.getString("status")),
                jsonObject.getBoolean("isAdult"),
                Collections.unmodifiableList(genresList),
                jsonObject.isNull("episodes") ? null : jsonObject.getInt("episodes"),
                jsonObject.isNull("nextAiringEpisode") ? null : jsonObject.getJSONObject("nextAiringEpisode").getInt("episode") - 1,
                jsonObject.isNull("nextAiringEpisode") ? null : Instant.ofEpochSecond(jsonObject.getJSONObject("nextAiringEpisode").getInt("airingAt")),
                jsonObject.getInt("averageScore")
        );
    }

    private static String extractTitle(JSONObject titleJson) {
        if (!titleJson.isNull("english")) {
            return titleJson.getString("english");
        } else if (!titleJson.isNull("romaji")) {
            return titleJson.getString("romaji");
        } else if (!titleJson.isNull("native")) {
            return titleJson.getString("native");
        } else {
            return "";
        }
    }

}
