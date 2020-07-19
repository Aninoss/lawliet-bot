package Modules.Reddit;

import Constants.Category;
import Constants.Locales;
import Core.Internet.HttpResponse;
import Core.Internet.InternetCache;
import Core.TextManager;
import Core.Utils.InternetUtil;
import Core.Utils.StringUtil;
import Modules.PostBundle;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class RedditDownloader {

    private final static Logger LOGGER = LoggerFactory.getLogger(RedditDownloader.class);
    private final static int TIMEOUT_MIN = 60;
    private static Instant nextRequestBlockUntil = null;

    public static RedditPost getImagePost(Locale locale, String sub) throws IOException, InterruptedException, ExecutionException {
        sub = URLEncoder.encode(sub, "UTF-8");

        RedditPost redditPost;
        int i = 0;
        do {
            redditPost = getPost(locale, sub);
            i++;
            if (i >= 50) break;
        } while (redditPost == null || redditPost.getImage() == null || !InternetUtil.urlContainsImage(redditPost.getImage()));

        return redditPost;
    }

    public static RedditPost getPost(Locale locale, String sub) throws IOException, InterruptedException, ExecutionException {
        if (nextRequestBlockUntil != null && Instant.now().isBefore(nextRequestBlockUntil))
            return null;

        if (sub.startsWith("r/")) sub = sub.substring(2);
        sub = URLEncoder.encode(sub, "UTF-8");

        Subreddit subreddit = SubredditContainer.getInstance().get(sub);
        String postReference = subreddit.getPostReference();
        if (postReference.length() > 0) postReference = "&after=" + postReference;

        String downloadUrl = "https://www.reddit.com/r/" + sub + ".json?raw_json=1" + postReference;

        HttpResponse httpResponse = InternetCache.getDataShortLived(downloadUrl).get();
        if (!httpResponse.getContent().isPresent()) {
            return null;
        }

        String dataString = httpResponse.getContent().get();
        if (!dataString.startsWith("{")) {
            return null;
        }

        JSONObject root = new JSONObject(dataString);
        if (root.has("error") && root.getInt("error") == 429) {
            nextRequestBlockUntil = Instant.now().plus(TIMEOUT_MIN, ChronoUnit.MINUTES);
            return null;
        }

        JSONObject tempData = root.getJSONObject("data");
        if (!tempData.isNull("after")) postReference = tempData.getString("after");
        else postReference = "";

        JSONArray postData = tempData.getJSONArray("children");
        if (postData.length() <= 0) {
            return null;
        }

        JSONObject data = postData.getJSONObject(subreddit.getRemainingIndex(postReference, postData.length())).getJSONObject("data");

        return getPost(locale, data);
    }

    public static PostBundle<RedditPost> getPostTracker(Locale locale, String sub, String arg) throws IOException, InterruptedException, ExecutionException {
        if (nextRequestBlockUntil != null && Instant.now().isBefore(nextRequestBlockUntil))
            return null;

        if (sub.startsWith("r/")) sub = sub.substring(2);
        sub = URLEncoder.encode(sub, "UTF-8");

        String downloadUrl = "https://www.reddit.com/r/" + sub + ".json?raw_json=1";

        HttpResponse httpResponse = InternetCache.getData(downloadUrl, 60 * 9).get();
        if (!httpResponse.getContent().isPresent()) return null;

        String dataString = httpResponse.getContent().get();
        if (!dataString.startsWith("{")) return null;

        JSONObject root = new JSONObject(dataString);
        if (root.has("error") && root.getInt("error") == 429) {
            nextRequestBlockUntil = Instant.now().plus(TIMEOUT_MIN, ChronoUnit.MINUTES);
            return null;
        }

        JSONArray postData = root.getJSONObject("data").getJSONArray("children");
        if (postData.length() <= 0) return null;

        ArrayList<RedditPost> redditPosts = new ArrayList<>();
        StringBuilder newArg = new StringBuilder();
        String[] postedArray;
        if (arg != null) postedArray = arg.split("\\|");
        else postedArray = new String[0];
        boolean stop = false;

        for (int i = postData.length() - 1; i >= 0; i--) {
            String name = postData.getJSONObject(i).getJSONObject("data").getString("name");
            if (i < postData.length() - 1) newArg.append("|");
            newArg.append(name);

            if (postedArray.length > 0 && name.equalsIgnoreCase(postedArray[postedArray.length-1])) stop = true;

            if (!stop) {
                boolean newPost = true;
                for (String postedName : postedArray) {
                    if (postedName.equalsIgnoreCase(name)) {
                        newPost = false;
                        break;
                    }
                }

                if (newPost) {
                    RedditPost post = getPost(locale, postData.getJSONObject(i).getJSONObject("data"));
                    if (post == null) return null;
                    redditPosts.add(post);
                }
            }
        }

        return new PostBundle<>(redditPosts, newArg.toString());
    }

    public static boolean checkRedditConnection() {
        try {
            return getPost(new Locale(Locales.EN), "memes") != null;
        } catch (IOException | InterruptedException | ExecutionException e) {
            LOGGER.error("Error in reddit check", e);
        }
        return false;
    }

    private static RedditPost getPost(Locale locale, JSONObject data) {
        RedditPost post = new RedditPost();

        String description = "", url = "", source = "", thumbnail = "", domain = "";
        Object flair;

        if (data.has("subreddit_name_prefixed")) post.setSubreddit(data.getString("subreddit_name_prefixed"));
        post.setScore(data.has("score") ? data.getInt("score") : 0);
        post.setComments(data.has("num_comments") ? data.getInt("num_comments") : 0);
        post.setInstant(new Date(data.getLong("created_utc") * 1000L).toInstant());

        if (!data.has("over_18")) return null;

        post.setNsfw(data.getBoolean("over_18"));
        post.setTitle(StringUtil.shortenString(data.getString("title"), 256));
        post.setAuthor(data.getString("author"));

        flair = data.get("link_flair_text");
        if (flair != null && !("" + flair).equals("null") && !("" + flair).equals("") && !("" + flair).equals(" "))
            post.setFlair(flair.toString());
        description = data.getString("selftext");
        url = data.getString("url");
        post.setLink(url);
        source = "https://www.reddit.com" + data.getString("permalink");
        thumbnail = data.getString("thumbnail");
        if (url.contains("//")) {
            domain = url.split("//")[1].replace("www.", "");
            if (domain.contains("/")) domain = domain.split("/")[0];
        }
        boolean postSource = true;

        if (data.has("post_hint") && data.getString("post_hint").equals("image")) {
            post.setImage(url);
            post.setLink(source);
            postSource = false;
            domain = "reddit.com";
        } else {
            if (data.has("preview") && data.getJSONObject("preview").has("images")) {
                post.setThumbnail(data.getJSONObject("preview").getJSONArray("images").getJSONObject(0).getJSONObject("source").getString("url"));
            } else {
                if (InternetUtil.urlContainsImage(url)) {
                    post.setImage(url);
                    post.setLink(source);
                    postSource = false;
                    domain = "reddit.com";
                } else if (thumbnail.toLowerCase().startsWith("http")) post.setThumbnail(thumbnail);
            }
        }

        if (postSource && !source.equals(url)) {
            String linkText = TextManager.getString(locale, Category.EXTERNAL, "reddit_linktext", source);
            description = StringUtil.shortenString(description, 2048-linkText.length());
            if (!description.equals("")) description += "\n\n";
            description += linkText;
        } else description = StringUtil.shortenString(description, 2048);

        post.setDescription(description);
        post.setDomain(domain);

        return post;
    }
}
