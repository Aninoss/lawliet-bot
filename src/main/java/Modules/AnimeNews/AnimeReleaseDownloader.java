package Modules.AnimeNews;

import Constants.Language;
import Core.Internet.InternetCache;
import Core.Internet.InternetResponse;
import Modules.PostBundle;
import Core.Tools.StringTools;
import Core.Tools.TimeTools;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AnimeReleaseDownloader {

    public static PostBundle<AnimeReleasePost> getPosts(Locale locale, String newestPostId, String filter) throws InterruptedException, ExecutionException {
        filter = StringTools.trimString(filter);

        String downloadUrl;
        if (StringTools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.crunchyroll.com/rss/anime?lang=deDE";
        else downloadUrl = "https://www.crunchyroll.com/rss/anime?lang=enUS";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 29 * 60).get();
        String postString = internetResponse.getContent().get();

        JSONArray postArray = XML.toJSONObject(postString).getJSONObject("rss").getJSONObject("channel").getJSONArray("item");
        ArrayList<AnimeReleasePost> postList = new ArrayList<>();
        List<AnimeReleasePost> animeReleasePosts = getAnimeReleasePostList(postArray, locale);

        List<Integer> currentUsedIds = newestPostId == null || newestPostId.isEmpty() ? new ArrayList<>() : Arrays.stream(newestPostId.split("\\|")).map(Integer::parseInt).collect(Collectors.toList());
        ArrayList<String> newUsedIds = new ArrayList<>();

        for(int i = 0; i < animeReleasePosts.size(); i++) {
            AnimeReleasePost post = animeReleasePosts.get(i);

            if (postPassesFilter(post, filter)) {
                if (!currentUsedIds.contains(post.getId()) &&
                        (postList.size() == 0 || newestPostId != null)
                ) postList.add(post);
                newUsedIds.add(String.valueOf(post.getId()));
            }
        }

        StringBuilder sb = new StringBuilder();
        newUsedIds.forEach(str -> sb.append("|").append(str));
        if (sb.length() > 0) newestPostId = sb.substring(1);
        else newestPostId = "";

        return new PostBundle<>(postList, newestPostId);
    }

    private static boolean postPassesFilter(AnimeReleasePost post, String filter) {
        return filter.equalsIgnoreCase("all") || post.getAnime().toLowerCase().contains(filter.toLowerCase()) || post.getUrl().equalsIgnoreCase(filter);
    }

    private static List<AnimeReleasePost> getAnimeReleasePostList(JSONArray data, Locale locale) {
        ArrayList<AnimeReleasePost> list = new ArrayList<>();

        for(int i = 0; i < data.length(); i++) {
            AnimeReleasePost post = parseEpisode(data.getJSONObject(i), locale);
            AnimeReleasePost nextPost = null;
            AnimeReleasePost tempPost;

            while(i + 1 < data.length() && (tempPost = parseEpisode(data.getJSONObject(i + 1), locale)).getAnime().equals(post.getAnime())) {
                nextPost = tempPost;
                i++;
            }

            if (nextPost != null) {
                String episode = null;
                if (nextPost.getEpisode().isPresent() && post.getEpisode().isPresent()) {
                    episode = String.format("%s - %s", nextPost.getEpisode().get(), post.getEpisode().get());
                }

                post = new AnimeReleasePost(
                        post.getAnime(),
                        "",
                        episode,
                        "",
                        post.getThumbnail(),
                        post.getDate(),
                        post.getUrl(),
                        post.getId()
                );
            }

            list.add(post);
        }

        return list;
    }

    private static AnimeReleasePost parseEpisode(JSONObject data, Locale locale) {
        String anime = data.getString("title");
        if (anime.contains(" - Episode ")) anime = anime.substring(0, anime.indexOf(" - Episode "));
        else if (anime.contains(" - Folge ")) anime = anime.substring(0, anime.indexOf(" - Folge "));
        else anime = data.getString("crunchyroll:seriesTitle");

        String description = data.getString("description");
        description = description.substring(description.indexOf("<br />") + "<br />".length());

        String episode = null;
        if (data.has("crunchyroll:episodeNumber")) {
            try {
                episode = StringTools.numToString(locale, data.getInt("crunchyroll:episodeNumber"));
            } catch (Exception e) {
                //Ignore
                episode = data.getString("crunchyroll:episodeNumber");
            }
        }

        String episodeTitle;
        try {
            episodeTitle = data.getString("crunchyroll:episodeTitle");
        } catch (Exception e) {
            //Ignore
            double value = data.getDouble("crunchyroll:episodeTitle");
            if (((int) value) == value) episodeTitle = String.valueOf((int) value);
            else episodeTitle = String.valueOf(value);
        }

        String thumbnail = "";
        if (data.has("media:thumbnail")) thumbnail = data.getJSONArray("media:thumbnail").getJSONObject(0).getString("url");
        Instant date = TimeTools.parseDateString2(data.getString("crunchyroll:premiumPubDate"));
        String url = data.getString("link");
        if (StringTools.getLanguage(locale) == Language.EN) url = url.replace("/de/", "/");
        int id = data.getInt("crunchyroll:mediaId");

        return new AnimeReleasePost(anime, description, episode, episodeTitle, thumbnail, date, url, id);
    }

}
