package General.AnimeNews;

import Constants.Language;
import General.Internet.InternetCache;
import General.Internet.InternetResponse;
import General.PostBundle;
import General.Tools;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class AnimeNewsDownloader {

    public static AnimeNewsPost getPost(Locale locale) throws IOException, InterruptedException, ExecutionException {
        String downloadUrl;
        if (Tools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.animenachrichten.de/";
        else downloadUrl = "https://www.animenewsnetwork.com/news/";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 60 * 14).get();
        if (!internetResponse.getContent().isPresent()) return null;
        String dataString = internetResponse.getContent().get();

        if (Tools.getLanguage(locale) == Language.DE) return getPostDE(getCurrentPostStringDE(dataString));
        else return getPostEN(getCurrentPostStringEN(dataString));
    }

    public static PostBundle<AnimeNewsPost> getPostTracker(Locale locale, String newestPostId) throws InterruptedException, ExecutionException {
        String downloadUrl;
        if (Tools.getLanguage(locale) == Language.DE) downloadUrl = "https://www.animenachrichten.de/";
        else downloadUrl = "https://www.animenewsnetwork.com/news/";

        InternetResponse internetResponse = InternetCache.getData(downloadUrl, 60 * 14).get();
        if (!internetResponse.getContent().isPresent()) return null;
        String dataString = internetResponse.getContent().get();

        ArrayList<AnimeNewsPost> postList = new ArrayList<>();
        for(int i=0; i < 5; i++) {
            String postString;
            if (Tools.getLanguage(locale) == Language.DE) postString = getCurrentPostStringDE(dataString);
            else postString = getCurrentPostStringEN(dataString);

            AnimeNewsPost post;
            try {
                if (Tools.getLanguage(locale) == Language.DE) post = getPostDE(postString);
                else post = getPostEN(postString);
            } catch (NullPointerException e) {
                //Ignore
                return null;
            }
            if (post.getId().equals(newestPostId)) break;

            postList.add(post);
            if (Tools.getLanguage(locale) == Language.DE) dataString = dataString.replaceFirst("class=\"td-block-span12\">", "");
            else dataString = dataString.replaceFirst("<div class=\"herald box news\"", "");
        }

        ArrayList<AnimeNewsPost> postSendList = new ArrayList<>();
        if (newestPostId != null) {
            for(int i = postList.size()-1; i >= 0; i--) {
                postSendList.add(postList.get(i));
            }
        } else if (postList.size() > 0)  {
            postSendList.add(postList.get(0));
        }

        if (postList.size() > 0) newestPostId = postList.get(0).getId();
        return new PostBundle<>(postSendList, newestPostId);
    }

    private static AnimeNewsPost getPostDE(String data) {
        AnimeNewsPost post = new AnimeNewsPost();

        post.setTitle(Tools.decryptString(Tools.cutString(data, "title=\"", "\"")));
        post.setDescription(Tools.decryptString(Tools.cutString(data + "</div>", "<div class=\"td-excerpt\">", "</div>")));
        post.setImage(Tools.cutString(data, "data-lazy-srcset=\"", " "));
        post.setLink(Tools.cutString(data, "<a href=\"", "\""));

        if (data.contains("#comments\">")) post.setComments(Integer.parseInt(Tools.cutString(data, "#comments\">", "<")));
        else post.setComments(Integer.parseInt(Tools.cutString(data, "#respond\">", "<")));

        post.setAuthor(Tools.decryptString(Tools.cutString(data, "class=\"td-post-author-name\">", "</a>").split(">")[1]));
        post.setDate(Tools.decryptString(Tools.cutString(data, "datetime=\"", "</time>").split(">")[1]));
        post.setId(Tools.cutString(data, "datetime=\"", "\""));
        post.setCategory("");

        return post;
    }

    private static AnimeNewsPost getPostEN(String data) {
        AnimeNewsPost post = new AnimeNewsPost();

        data = data.replace("<cite>", "").replace("</cite>", "").replaceFirst("&amp;from=I.MF\">", "").replaceFirst("<a href=\"", "");

        post.setTitle(Tools.decryptString(Tools.cutString(data, "&amp;from=I.MF\">", "</a>")));
        post.setDescription(Tools.decryptString(Tools.cutString(data, "<span class=\"full\">― ", "</span>")));
        post.setImage("https://www.animenewsnetwork.com" + Tools.cutString(data, "data-src=\"", "\">"));
        post.setLink("https://www.animenewsnetwork.com" + Tools.cutString(data, "<a href=\"", "\""));
        post.setComments(Integer.parseInt(Tools.cutString(Tools.cutString(data, "<div class=\"comments\"><a href=\"", "</a></div>"), ">", " ")));
        post.setAuthor("");
        post.setDate(Tools.decryptString(Tools.cutString(Tools.cutString(data, "<time datetime=\"", "/time>"), ">", "<")));
        post.setId(Tools.cutString(data, "data-track=\"id=", "</a>"));
        post.setCategory(Tools.decryptString(Tools.cutString(data, "<span class=\"topics\">", "</div>")));

        return post;
    }

    private static String getCurrentPostStringDE(String str) {
        if (!str.contains("class=\"td-block-span12\">")) return null;
        return Tools.cutString(str, "class=\"td-block-span12\">", "</div></div></div>");
    }

    private static String getCurrentPostStringEN(String str) {
        if (!str.contains("<div class=\"herald box news\"")) return null;
        return Tools.cutString(str, "<div class=\"herald box news\"", "<div class=\"herald box news\"");
    }
}
