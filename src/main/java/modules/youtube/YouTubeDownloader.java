package modules.youtube;

import constants.RegexPatterns;
import core.internet.HttpCache;
import core.internet.HttpResponse;
import core.utils.StringUtil;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class YouTubeDownloader {

    private static final String[] CHANNEL_FINDER_KEYS = new String[]{"data-channel-external-id=\"", "\"externalId\":\""};

    public static List<YouTubeVideo> retrieveVideos(String handle) throws ExecutionException, InterruptedException {
        if (!handle.startsWith("@")) {
            handle = "@" + handle;
        }
        if (!RegexPatterns.YOUTUBE_HANDLE.matcher(handle).matches()) {
            return Collections.emptyList();
        }

        HttpResponse httpResponse = HttpCache.get("https://www.youtube.com/" + handle, Duration.ofDays(7)).get();
        if (httpResponse.getCode() / 100 != 2) {
            return Collections.emptyList();
        }

        String channelId = extractChannelId(httpResponse.getBody());
        if (channelId == null) {
            return null;
        }

        JSONArray jsonArray = retrieveJsonArray("https://www.youtube.com/feeds/videos.xml?channel_id=" + channelId);
        ArrayList<YouTubeVideo> posts = new ArrayList<>();
        for (int i = 0; i < Math.min(5, jsonArray.length()); i++) {
            JSONObject jsonVideo = jsonArray.getJSONObject(i);
            posts.add(extractVideo(jsonVideo));
        }

        return posts;
    }

    private static String extractChannelId(String body) {
        for (String channelFinderKey : CHANNEL_FINDER_KEYS) {
            if (body.contains(channelFinderKey)) {
                return StringUtil.extractGroups(body, channelFinderKey, "\"")[0];
            }
        }
        return null;
    }

    private static JSONArray retrieveJsonArray(String downloadUrl) throws ExecutionException, InterruptedException {
        HttpResponse httpResponse = HttpCache.get(downloadUrl, Duration.ofMinutes(15)).get();
        if (httpResponse.getCode() / 100 != 2) {
            return null;
        }

        String content = httpResponse.getBody();
        return XML.toJSONObject(content)
                .getJSONObject("feed")
                .getJSONArray("entry");
    }

    private static YouTubeVideo extractVideo(JSONObject jsonVideo) {
        JSONObject authorJson = jsonVideo.getJSONObject("author");
        JSONObject mediaJson = jsonVideo.getJSONObject("media:group");
        JSONObject communityJson = mediaJson.getJSONObject("media:community");
        return new YouTubeVideo(
                StringUtil.shortenString(authorJson.getString("name"), MessageEmbed.AUTHOR_MAX_LENGTH),
                authorJson.getString("uri"),
                StringUtil.shortenString(jsonVideo.getString("title"), MessageEmbed.TITLE_MAX_LENGTH),
                mediaJson.getJSONObject("media:thumbnail").getString("url"),
                jsonVideo.getJSONObject("link").getString("href"),
                Instant.parse(jsonVideo.getString("published")),
                communityJson.getJSONObject("media:statistics").getLong("views"),
                communityJson.has("media:starRating") ? communityJson.getJSONObject("media:starRating").getLong("count") : null
        );
    }

}
