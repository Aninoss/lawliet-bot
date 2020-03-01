package General.AnimeNews;

import Constants.Language;
import General.Internet.InternetCache;
import General.Internet.InternetResponse;
import General.PostBundle;
import General.Tools;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class AnimeReleaseDownloader {

    public static AnimeReleasePost getPost(Locale locale) throws IOException, InterruptedException, ExecutionException {
        String downloadUrl;
        if (Tools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.crunchyroll.com/rss/anime?lang=deDE";
        else downloadUrl = "https://www.crunchyroll.com/rss/anime?lang=enUS";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 60 * 4);
        if (!internetResponse.getContent().isPresent()) return null;
        String dataString = internetResponse.getContent().get();

        JSONArray data = XML.toJSONObject(dataString).getJSONObject("rss").getJSONObject("channel").getJSONArray("item");
        return parse(data.getJSONObject(0), locale);
    }

    private static AnimeReleasePost parse(JSONObject data, Locale locale) {
        String anime = data.getString("title");
        if (anime.contains(" - Episode ")) anime = anime.substring(0, anime.indexOf(" - Episode "));
        else if (anime.contains(" - Folge ")) anime = anime.substring(0, anime.indexOf(" - Folge "));
        else anime = data.getString("crunchyroll:seriesTitle");

        String description = data.getString("description");
        description = description.substring(description.indexOf("<br />") + "<br />".length());

        String episode = null;
        if (data.has("crunchyroll:episodeNumber")) {
            try {
                episode = Tools.numToString(locale, data.getInt("crunchyroll:episodeNumber"));
            } catch (Throwable e) {
                //Ignore
                episode = data.getString("crunchyroll:episodeNumber");
            }
        }

        String episodeTitle;
        try {
            episodeTitle = data.getString("crunchyroll:episodeTitle");
        } catch (Throwable e) {
            //Ignore
            double value = data.getDouble("crunchyroll:episodeTitle");
            if (((int) value) == value) episodeTitle = String.valueOf((int) value);
            else episodeTitle = String.valueOf(value);
        }

        String thumbnail = "";
        if (data.has("media:thumbnail")) thumbnail = data.getJSONArray("media:thumbnail").getJSONObject(0).getString("url");
        Instant date = Tools.parseDateString2(data.getString("crunchyroll:premiumPubDate"));
        String url = data.getString("link");
        if (Tools.getLanguage(locale) == Language.EN) url = url.replace("/de/", "/");
        int id = data.getInt("crunchyroll:mediaId");

        return new AnimeReleasePost(anime, description, episode, episodeTitle, thumbnail, date, url, id);
    }

    public static PostBundle<AnimeReleasePost> getPostTracker(Locale locale, String newestPostId) throws IOException, InterruptedException, ExecutionException {
        String downloadUrl;
        if (Tools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.crunchyroll.com/rss/anime?lang=deDE";
        else downloadUrl = "https://www.crunchyroll.com/rss/anime?lang=enUS";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 60 * 4);
        if (!internetResponse.getContent().isPresent()) return null;
        String dataString = internetResponse.getContent().get();

        JSONArray dataArray = XML.toJSONObject(dataString).getJSONObject("rss").getJSONObject("channel").getJSONArray("item");
        ArrayList<AnimeReleasePost> postList = new ArrayList<>();
        for(int i = 0; i < 10; i++) {
            AnimeReleasePost post = parse(dataArray.getJSONObject(i), locale);
            if (String.valueOf(post.getId()).equals(newestPostId)) break;

            postList.add(post);
        }

        ArrayList<AnimeReleasePost> postSendList = new ArrayList<>();
        if (newestPostId != null) {
            for(int i = postList.size()-1; i >= 0; i--) {
                postSendList.add(postList.get(i));
            }
        }

        if (postList.size() > 0) newestPostId = String.valueOf(postList.get(0).getId());
        return new PostBundle<>(postSendList, newestPostId);
    }
}
