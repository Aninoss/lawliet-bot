package modules.animenews;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import constants.Language;
import core.MainLogger;
import core.internet.HttpResponse;
import core.internet.InternetCache;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.PostBundle;

public class AnimeNewsDownloader {

    public static AnimeNewsPost getPost(Locale locale) throws InterruptedException, ExecutionException {
        String dataString = getDataString(locale);

        if (StringUtil.getLanguage(locale) == Language.DE) {
            return getPostDE(getCurrentPostStringDE(dataString)[0]);
        } else {
            return getPostEN(getCurrentPostStringEN(dataString)[0]);
        }
    }

    public static PostBundle<AnimeNewsPost> getPostTracker(Locale locale, String newestTimeString) throws InterruptedException, ExecutionException {
        String dataString = getDataString(locale);

        ArrayList<AnimeNewsPost> postList = new ArrayList<>();
        String[] postStrings;
        if (StringUtil.getLanguage(locale) == Language.DE) {
            postStrings = getCurrentPostStringDE(dataString);
        } else {
            postStrings = getCurrentPostStringEN(dataString);
        }

        Instant compareTime;
        try {
            compareTime = newestTimeString == null || newestTimeString.isEmpty() ? new Date(0).toInstant() : Instant.parse(newestTimeString);
        } catch (DateTimeParseException e) {
            MainLogger.get().error("Could not parse post date", e);
            compareTime = Instant.now();
        }
        Instant newestTime = compareTime;

        for (int i = 0; i < 5; i++) {
            String postString = postStrings[i];

            AnimeNewsPost post;
            try {
                if (StringUtil.getLanguage(locale) == Language.DE) {
                    post = getPostDE(postString);
                } else {
                    post = getPostEN(postString);
                }
            } catch (NullPointerException e) {
                MainLogger.get().error("Could not extract news post", e);
                return null;
            }

            if (post.getInstant().isAfter(compareTime)) {
                if (post.getInstant().isAfter(newestTime)) newestTime = post.getInstant();
                if (i == 0 || newestTimeString != null) postList.add(post);
            }
        }

        Collections.reverse(postList);

        return new PostBundle<>(postList, newestTime.toString());
    }

    private static AnimeNewsPost getPostDE(String data) {
        AnimeNewsPost post = new AnimeNewsPost();

        post.setTitle(StringUtil.decryptString(StringUtil.extractGroups(data, "title=\"", "\"")[0]));
        post.setDescription(StringUtil.decryptString(StringUtil.extractGroups(data + "</div>", "<div class=\"td-excerpt\">", "</div>")[0]));
        post.setImage(StringUtil.extractGroups(data, "data-lazy-srcset=\"", " ")[0]);
        post.setLink(StringUtil.extractGroups(data, "<a href=\"", "\"")[0]);

        if (data.contains("#comments\">")) {
            post.setComments(Integer.parseInt(StringUtil.extractGroups(data, "#comments\">", "<")[0]));
        } else {
            post.setComments(Integer.parseInt(StringUtil.extractGroups(data, "#respond\">", "<")[0]));
        }

        String dateString = StringUtil.extractGroups(data, "datetime=\"", "\"")[0];
        Instant instant = TimeUtil.parseDateString3(dateString);
        post.setInstant(instant);

        post.setAuthor("");
        post.setCategory("");

        return post;
    }

    private static AnimeNewsPost getPostEN(String data) {
        AnimeNewsPost post = new AnimeNewsPost();

        data = data.replace("<cite>", "").replace("</cite>", "").replaceFirst("&amp;from=I.MF\">", "").replaceFirst("<a href=\"", "");

        post.setTitle(StringUtil.decryptString(StringUtil.extractGroups(data, "&amp;from=I.MF\">", "</a>")[0]));
        post.setDescription(StringUtil.decryptString(StringUtil.extractGroups(data, "<span class=\"full\">― ", "</span>")[0]));
        post.setImage("https://www.animenewsnetwork.com" + StringUtil.extractGroups(data, "data-src=\"", "\">")[0]);
        post.setLink("https://www.animenewsnetwork.com" + StringUtil.extractGroups(data, "<a href=\"", "\"")[0]);
        post.setComments(Integer.parseInt(StringUtil.extractGroups(StringUtil.extractGroups(data, "<div class=\"comments\"><a href=\"", "</a></div>")[0], ">", " ")[0]));
        post.setAuthor("");

        String dateString = StringUtil.extractGroups(data, "<time datetime=\"", "\"")[0];
        Instant instant = TimeUtil.parseDateString3(dateString);
        post.setInstant(instant);

        post.setCategory(StringUtil.decryptString(StringUtil.extractGroups(data, "<span class=\"topics\">", "</div>")[0]));

        return post;
    }

    private static String[] getCurrentPostStringDE(String str) {
        return StringUtil.extractGroups(str, "class=\"td-block-span12\">", "</div></div></div>");
    }

    private static String[] getCurrentPostStringEN(String str) {
        return StringUtil.extractGroups(str, "<div class=\"herald box news\"", "<div class=\"herald box news\"");
    }

    private static String getDataString(Locale locale) throws ExecutionException, InterruptedException {
        String downloadUrl;
        if (StringUtil.getLanguage(locale) == Language.DE) {
            downloadUrl = "https://www.animenachrichten.de/";
        } else {
            downloadUrl = "https://www.animenewsnetwork.com/news/";
        }

        HttpResponse httpResponse = InternetCache.getData(downloadUrl, 60 * 14).get();
        if (httpResponse.getContent().isEmpty()) return null;
        return httpResponse.getContent().get();
    }

}
