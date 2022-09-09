package modules.mandaupdates;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import core.internet.HttpCache;
import core.internet.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class MangaUpdatesDownloader {

    private static final String[] NSFW_GENRES = { "Hentai", "Lolicon", "Shotacon", "Yaoi", "Yuri" };

    public static List<MangaUpdatesSeries> searchSeries(String search) throws ExecutionException, InterruptedException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("search", search.toLowerCase());
        jsonBody.put("perpage", 25);

        String response = HttpCache.post("https://api.mangaupdates.com/v1/series/search", jsonBody.toString(), "application/json", Duration.ofHours(1)).get()
                .getBody();
        JSONArray resultsJson = new JSONObject(response).getJSONArray("results");

        ArrayList<MangaUpdatesSeries> series = new ArrayList<>();
        for (int i = 0; i < resultsJson.length(); i++) {
            JSONObject recordJson = resultsJson.getJSONObject(i).getJSONObject("record");
            JSONObject imageUrlJson = recordJson.getJSONObject("image").getJSONObject("url");
            long seriesId = recordJson.getLong("series_id");
            String title = recordJson.getString("title");
            String image = imageUrlJson.isNull("thumb") ? null : imageUrlJson.getString("thumb");
            String url = recordJson.getString("url");
            boolean nsfw = seriesIsNsfw(recordJson);
            series.add(new MangaUpdatesSeries(seriesId, title, image, url, nsfw));
        }

        return series;
    }

    public static List<MangaUpdatesRelease> getReleasesOfSeries(long seriesId) throws ExecutionException, InterruptedException {
        HttpResponse httpResponse = HttpCache.get("https://api.mangaupdates.com/v1/series/" + seriesId + "/rss", Duration.ofMinutes(15)).get();
        if (httpResponse.getCode() != 200) {
            return Collections.emptyList();
        }

        String response = httpResponse.getBody();
        JSONObject channelJson = XML.toJSONObject(response).getJSONObject("rss").getJSONObject("channel");
        if (!channelJson.has("item")) {
            return Collections.emptyList();
        }

        if (channelJson.get("item") instanceof JSONArray) {
            JSONArray releasesJson = channelJson.getJSONArray("item");
            ArrayList<MangaUpdatesRelease> releases = new ArrayList<>();
            for (int i = 0; i < releasesJson.length(); i++) {
                JSONObject releaseJson = releasesJson.getJSONObject(i);
                String title = releaseJson.getString("title");
                String description = releaseJson.getString("description");
                releases.add(new MangaUpdatesRelease(title, description));
            }
            return releases;
        } else if (channelJson.get("item") instanceof JSONObject) {
            JSONObject releaseJson = channelJson.getJSONObject("item");
            String title = releaseJson.getString("title");
            String description = releaseJson.getString("description");
            return Collections.singletonList(new MangaUpdatesRelease(title, description));
        } else {
            return Collections.emptyList();
        }
    }

    private static boolean seriesIsNsfw(JSONObject recordJson) {
        if (recordJson.isNull("genres")) {
            return false;
        }

        JSONArray genresJson = recordJson.getJSONArray("genres");
        for (int i = 0; i < genresJson.length(); i++) {
            String genre = genresJson.getJSONObject(i).getString("genre");
            if (Arrays.stream(NSFW_GENRES).anyMatch(genre::equalsIgnoreCase)) {
                return true;
            }
        }

        return false;
    }

}
