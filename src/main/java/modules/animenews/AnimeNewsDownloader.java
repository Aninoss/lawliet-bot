package modules.animenews;

import constants.Language;
import core.LocalFile;
import core.MainLogger;
import core.internet.HttpCache;
import core.internet.HttpResponse;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class AnimeNewsDownloader {

    public static List<AnimeNewsArticle> retrieveArticles(Locale locale) throws ExecutionException, InterruptedException {
        Language language = Language.from(locale);
        String downloadUrl = switch (language) {
            case DE -> "https://www.anime2you.de/news/feed/";
            case ES -> "https://feeds.feedburner.com/animanga_rss";
            default -> "https://www.animenewsnetwork.com/all/rss.xml?ann-edition=w";
        };

        JSONArray jsonArray = retrieveJSONArray(downloadUrl);
        if (jsonArray == null) return null;

        ArrayList<AnimeNewsArticle> posts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JSONObject jsonPost = jsonArray.getJSONObject(i);
            AnimeNewsArticle article = switch (language) {
                case DE -> extractPostDe(jsonPost);
                case ES -> extractPostEs(jsonPost);
                default -> extractPostEn(jsonPost);
            };
            posts.add(article);
        }

        return posts;
    }

    private static JSONArray retrieveJSONArray(String downloadUrl) throws ExecutionException, InterruptedException {
        HttpResponse httpResponse = HttpCache.get(downloadUrl, Duration.ofMinutes(15)).get();
        if (httpResponse.getCode() / 100 != 2) return null;

        String content = httpResponse.getBody();
        return XML.toJSONObject(content)
                .getJSONObject("rss")
                .getJSONObject("channel")
                .getJSONArray("item");
    }

    private static AnimeNewsArticle extractPostEs(JSONObject jsonPost) {
        String contentEncoded = jsonPost.getString("content:encoded");
        String thumbnailUrl;
        try {
            thumbnailUrl = StringUtil.extractGroups(contentEncoded, "<img src=\"", "\"")[0];
        } catch (Throwable e) {
            MainLogger.get().error("Anime news spanish missing thumbnail", e);
            thumbnailUrl = null;
        }

        return new AnimeNewsArticle(
                StringUtil.shortenString(StringUtil.unescapeHtml(jsonPost.getString("title")), 256),
                StringUtil.unescapeHtml(jsonPost.getString("description")),
                thumbnailUrl,
                jsonPost.getString("link"),
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

    private static AnimeNewsArticle extractPostEn(JSONObject jsonPost) {
        String link = jsonPost.getString("link");
        String[] linkArray = link.split("/");
        LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("temp/animenews_%s.jpg", linkArray[linkArray.length - 1]));

        if (!tempFile.exists()) {
            try {
                tempFile.createNewFile();
                String thumbnailUrl = InternetUtil.retrieveThumbnailPreview(jsonPost.getString("link")).get();
                InternetUtil.downloadFile(thumbnailUrl, tempFile);
            } catch (InterruptedException | ExecutionException | IOException e) {
                MainLogger.get().error("Anime news thumbnail download error", e);
            }
        }

        return new AnimeNewsArticle(
                StringUtil.shortenString(StringUtil.unescapeHtml(jsonPost.getString("title")), 256),
                StringUtil.unescapeHtml(jsonPost.getString("description")),
                tempFile.cdnGetUrl(),
                link,
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

    private static AnimeNewsArticle extractPostDe(JSONObject jsonPost) {
        String content = jsonPost.getString("description");

        return new AnimeNewsArticle(
                StringUtil.shortenString(StringUtil.unescapeHtml(jsonPost.getString("title")), 256),
                StringUtil.unescapeHtml(StringUtil.extractGroups(content, "</p>\n<p>", "</p>")[0]),
                content.contains("src=\"") ? StringUtil.unescapeHtml(StringUtil.extractGroups(content, "src=\"", "\"")[0]) : null,
                jsonPost.getString("link"),
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

}
