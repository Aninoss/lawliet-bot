package modules.animenews;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import constants.Language;
import core.MainLogger;
import core.internet.HttpResponse;
import core.internet.HttpCache;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

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
        HttpResponse httpResponse = HttpCache.getData(downloadUrl).get();
        if (httpResponse.getBody() == null) return null;

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
            thumbnailUrl = "https://media.redadn.es/" + StringUtil.extractGroups(contentEncoded, "https://media.redadn.es/", "\"")[0];
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
        String thumbnailUrl = null;
        try {
            thumbnailUrl = InternetUtil.retrieveThumbnailPreview(jsonPost.getString("link")).get();
        } catch (InterruptedException | ExecutionException e) {
            //Ignore
        }

        return new AnimeNewsArticle(
                StringUtil.shortenString(StringUtil.unescapeHtml(jsonPost.getString("title")), 256),
                StringUtil.unescapeHtml(jsonPost.getString("description")),
                thumbnailUrl,
                jsonPost.getString("link"),
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

    private static AnimeNewsArticle extractPostDe(JSONObject jsonPost) {
        String content = jsonPost.getString("description");

        return new AnimeNewsArticle(
                StringUtil.shortenString(StringUtil.unescapeHtml(jsonPost.getString("title")), 256),
                StringUtil.unescapeHtml(StringUtil.extractGroups(content, "</p><p>", "</p>")[0]),
                StringUtil.unescapeHtml(StringUtil.extractGroups(content, "src=\"", "\"")[0]),
                jsonPost.getString("link"),
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

}
