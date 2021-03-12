package modules.animenews;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import constants.Language;
import core.internet.HttpResponse;
import core.internet.InternetCache;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

public class AnimeNewsDownloader {

    public static List<AnimeNewsArticle> retrieveArticles(Locale locale) throws ExecutionException, InterruptedException {
        String downloadUrl;
        if (StringUtil.getLanguage(locale) == Language.DE) {
            downloadUrl = "https://www.anime2you.de/news/feed/";
        } else {
            downloadUrl = "https://www.animenewsnetwork.com/all/rss.xml?ann-edition=w";
        }

        JSONArray jsonArray = retrieveJSONArray(downloadUrl);
        if (jsonArray == null) return null;

        ArrayList<AnimeNewsArticle> posts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            JSONObject jsonPost = jsonArray.getJSONObject(i);
            if (StringUtil.getLanguage(locale) == Language.DE) {
                posts.add(extractPostDe(jsonPost));
            } else {
                posts.add(extractPostEn(jsonPost));
            }
        }

        return posts;
    }

    private static JSONArray retrieveJSONArray(String downloadUrl) throws ExecutionException, InterruptedException {
        HttpResponse httpResponse = InternetCache.getData(downloadUrl, 60 * 14).get();
        if (httpResponse.getContent().isEmpty()) return null;

        String content = httpResponse.getContent().get();
        return XML.toJSONObject(content)
                .getJSONObject("rss")
                .getJSONObject("channel")
                .getJSONArray("item");
    }

    private static AnimeNewsArticle extractPostEn(JSONObject jsonPost) throws ExecutionException, InterruptedException {
        return new AnimeNewsArticle(
                StringUtil.shortenString(StringEscapeUtils.unescapeHtml4(jsonPost.getString("title")), 256),
                StringEscapeUtils.unescapeHtml4(jsonPost.getString("description")),
                InternetUtil.retrieveThumbnailPreview(jsonPost.getString("link")).get(),
                jsonPost.getString("link"),
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

    private static AnimeNewsArticle extractPostDe(JSONObject jsonPost) {
        String content = jsonPost.getString("description");

        return new AnimeNewsArticle(
                StringUtil.shortenString(StringEscapeUtils.unescapeHtml4(jsonPost.getString("title")), 256),
                StringEscapeUtils.unescapeHtml4(StringUtil.extractGroups(content, "</p><p>", "</p>")[0]),
                StringEscapeUtils.unescapeHtml4(StringUtil.extractGroups(content, "src=\"", "\"")[0]),
                jsonPost.getString("link"),
                TimeUtil.parseDateStringRSS(jsonPost.getString("pubDate"))
        );
    }

}
