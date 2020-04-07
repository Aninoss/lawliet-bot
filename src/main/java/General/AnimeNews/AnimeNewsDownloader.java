package General.AnimeNews;

import Constants.Language;
import General.Internet.InternetCache;
import General.Internet.InternetResponse;
import General.PostBundle;
import General.Tools.StringTools;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class AnimeNewsDownloader {

    public static AnimeNewsPost getPost(Locale locale) throws InterruptedException, ExecutionException {
        String downloadUrl;
        if (StringTools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.animenachrichten.de/";
        else downloadUrl = "https://www.animenewsnetwork.com/news/";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 60 * 14).get();
        if (!internetResponse.getContent().isPresent()) return null;
        String dataString = internetResponse.getContent().get();

        if (StringTools.getLanguage(locale) == Language.DE) return getPostDE(getCurrentPostStringDE(dataString)[0]);
        else return getPostEN(getCurrentPostStringEN(dataString)[0]);
    }

    public static PostBundle<AnimeNewsPost> getPostTracker(Locale locale, String newestPostId) throws InterruptedException, ExecutionException {
        String downloadUrl;
        if (StringTools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.animenachrichten.de/";
        else downloadUrl = "https://www.animenewsnetwork.com/news/";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 60 * 14).get();
        if (!internetResponse.getContent().isPresent()) return null;
        String dataString = internetResponse.getContent().get();

        ArrayList<AnimeNewsPost> postList = new ArrayList<>();
        String[] postStrings;
        if (StringTools.getLanguage(locale) == Language.DE) postStrings = getCurrentPostStringDE(dataString);
        else postStrings = getCurrentPostStringEN(dataString);

        List<String> currentUsedIds = newestPostId == null ? new ArrayList<>() : Arrays.asList(newestPostId.split("//|"));
        ArrayList<String> newUsedIds = new ArrayList<>();

        for(int i = 0; i < 5; i++) {
            String postString = postStrings[i];

            AnimeNewsPost post;
            try {
                if (StringTools.getLanguage(locale) == Language.DE) post = getPostDE(postString);
                else post = getPostEN(postString);
            } catch (NullPointerException e) {
                e.printStackTrace();
                return null;
            }

            if (!currentUsedIds.contains(post.getId()) && (i == 0 || newestPostId != null))
                postList.add(post);
            newUsedIds.add(post.getId());
        }

        Collections.reverse(postList);

        StringBuilder sb = new StringBuilder();
        newUsedIds.forEach(str -> sb.append("|").append(str));
        if (sb.length() > 0) newestPostId = sb.substring(1);

        return new PostBundle<>(postList, newestPostId);
    }

    private static AnimeNewsPost getPostDE(String data) {
        AnimeNewsPost post = new AnimeNewsPost();

        post.setTitle(StringTools.decryptString(StringTools.extractGroups(data, "title=\"", "\"")[0]));
        post.setDescription(StringTools.decryptString(StringTools.extractGroups(data + "</div>", "<div class=\"td-excerpt\">", "</div>")[0]));
        post.setImage(StringTools.extractGroups(data, "data-lazy-srcset=\"", " ")[0]);
        post.setLink(StringTools.extractGroups(data, "<a href=\"", "\"")[0]);

        if (data.contains("#comments\">")) post.setComments(Integer.parseInt(StringTools.extractGroups(data, "#comments\">", "<")[0]));
        else post.setComments(Integer.parseInt(StringTools.extractGroups(data, "#respond\">", "<")[0]));

        post.setAuthor(StringTools.decryptString(StringTools.extractGroups(data, "class=\"td-post-author-name\">", "</a>")[0].split(">")[1]));
        post.setDate(StringTools.decryptString(StringTools.extractGroups(data, "datetime=\"", "</time>")[0].split(">")[1]));
        post.setId(StringTools.extractGroups(data, "datetime=\"", "\"")[0]);
        post.setCategory("");

        return post;
    }

    private static AnimeNewsPost getPostEN(String data) {
        AnimeNewsPost post = new AnimeNewsPost();

        data = data.replace("<cite>", "").replace("</cite>", "").replaceFirst("&amp;from=I.MF\">", "").replaceFirst("<a href=\"", "");

        post.setTitle(StringTools.decryptString(StringTools.extractGroups(data, "&amp;from=I.MF\">", "</a>")[0]));
        post.setDescription(StringTools.decryptString(StringTools.extractGroups(data, "<span class=\"full\">― ", "</span>")[0]));
        post.setImage("https://www.animenewsnetwork.com" + StringTools.extractGroups(data, "data-src=\"", "\">")[0]);
        post.setLink("https://www.animenewsnetwork.com" + StringTools.extractGroups(data, "<a href=\"", "\"")[0]);
        post.setComments(Integer.parseInt(StringTools.extractGroups(StringTools.extractGroups(data, "<div class=\"comments\"><a href=\"", "</a></div>")[0], ">", " ")[0]));
        post.setAuthor("");
        post.setDate(StringTools.decryptString(StringTools.extractGroups(StringTools.extractGroups(data, "<time datetime=\"", "/time>")[0], ">", "<")[0]));
        post.setId(StringTools.extractGroups(data, "data-track=\"id=", "</a>")[0]);
        post.setCategory(StringTools.decryptString(StringTools.extractGroups(data, "<span class=\"topics\">", "</div>")[0]));

        return post;
    }

    private static String[] getCurrentPostStringDE(String str) {
        return StringTools.extractGroups(str, "class=\"td-block-span12\">", "</div></div></div>");
    }

    private static String[] getCurrentPostStringEN(String str) {
        return StringTools.extractGroups(str, "<div class=\"herald box news\"", "<div class=\"herald box news\"");
    }
}
