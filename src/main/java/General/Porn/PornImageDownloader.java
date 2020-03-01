package General.Porn;

import General.Comment;
import General.Internet.InternetCache;
import General.Internet.InternetResponse;
import General.Tools;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PornImageDownloader {

    public static PornImage getPicture(String domain, String searchTerm, String searchTermExtra, String imageTemplate, boolean gifOnly, boolean canBeVideo, ArrayList<String> additionalFilters) throws IOException, InterruptedException, ExecutionException {
        return getPicture(domain, searchTerm, searchTermExtra, imageTemplate, gifOnly, canBeVideo, 2, false, additionalFilters);
    }

    public static PornImage getPicture(String domain, String searchTerm, String searchTermExtra, String imageTemplate, boolean gifOnly, boolean canBeVideo, int remaining, boolean softMode, ArrayList<String> additionalFilters) throws IOException, InterruptedException, ExecutionException {
        while(searchTerm.contains("  ")) searchTerm = searchTerm.replace("  ", " ");
        searchTerm = searchTerm.replace(", ", ",");
        searchTerm = searchTerm.replace("; ", ",");

        String searchTermEncoded = URLEncoder.encode(
                searchTerm
                        .replace(",", " ")
                        .replace(" ", softMode ? "~ " : " ") +
                        (softMode ? "~" : "") +
                        searchTermExtra, "UTF-8"
        );

        String url = "https://"+domain+"/index.php?page=dapi&s=post&q=index&tags=" + searchTermEncoded;
        String data = InternetCache.getData(url, 60 * 60).getContent().get();

        int count = Math.min(200*100, Integer.parseInt(Tools.cutString(data,"count=\"","\"")));

        if (count == 0) {
            if (!softMode) {
                return getPicture(domain, searchTerm.replace(" ", "_"), searchTermExtra, imageTemplate, gifOnly, canBeVideo, remaining, true, additionalFilters);
            } else if (remaining > 0) {
                if (searchTerm.contains(" "))
                    return getPicture(domain, searchTerm.replace(" ", "_"), searchTermExtra, imageTemplate, gifOnly, canBeVideo, remaining - 1, false, additionalFilters);
                else if (searchTerm.contains("_"))
                    return getPicture(domain, searchTerm.replace("_", " "), searchTermExtra, imageTemplate, gifOnly, canBeVideo, remaining - 1, false, additionalFilters);
            }

            return null;
        }

        Random r = new Random();
        int page = r.nextInt(count)/100;
        if (searchTermEncoded.length() == 0) page = 0;

        return getPictureOnPage(domain, searchTermEncoded, page, imageTemplate, gifOnly, canBeVideo, additionalFilters);
    }

    private static PornImage getPictureOnPage(String domain, String searchTerm, int page, String imageTemplate, boolean gifOnly, boolean canBeVideo, ArrayList<String> additionalFilters) throws IOException, InterruptedException, ExecutionException {
        String url = "https://"+domain+"/index.php?page=dapi&s=post&q=index&json=1&tags="+searchTerm+"&pid="+page;
        InternetResponse internetResponse = InternetCache.getData(url, 60 * 60);

        if (!internetResponse.getContent().isPresent()) return null;

        JSONArray data = new JSONArray(internetResponse.getContent().get());

        int count = Math.min(data.length(), 100);
        int count2 = 0;

        if (count == 0) {
            return null;
        }

        Random r = new Random();
        ArrayList<Long> scoreList = new ArrayList<>();
        ArrayList<Integer> posList = new ArrayList<>();
        long totalScore = 0;
        for (int i = 0; i < count; i++) {
            JSONObject postData = data.getJSONObject(i);
            String fileUrl = postData.getString(postData.has("file_url") ? "file_url" : "image");

            long score = 0;
            if ((Tools.UrlContainsImage(fileUrl) || canBeVideo)  && (!gifOnly || fileUrl.endsWith("gif")) && postData.getInt("score") >= 0 && !Tools.stringContainsBannedTags(postData.getString("tags"), additionalFilters)) {
                count2++;
                if (!PornImageCache.getInstance().contains(searchTerm, fileUrl)) {
                    score = (long) Math.pow(postData.getInt("score") + 1, 2.75);
                }
            }

            scoreList.add(score);
            posList.add(i);
            totalScore += score;
        }

        if (scoreList.size() == 0) return null;

        long pos = (long) (r.nextDouble()*totalScore);
        for(int i=0; i < scoreList.size(); i++) {
            pos -= scoreList.get(i);
            if (pos < 0) {
                JSONObject postData = data.getJSONObject(posList.get(i));
                String fileUrl = postData.getString(postData.has("file_url") ? "file_url" : "image");
                PornImageCache.getInstance().add(searchTerm, fileUrl, count2 - 1);
                return getSpecificPictureOnPage(domain, data, posList.get(i), imageTemplate);
            }
        }

        return null;
    }

    private static PornImage getSpecificPictureOnPage(String domain, JSONArray data, int pos, String imageTemplate) throws IOException, InterruptedException, ExecutionException {
        JSONObject postData = data.getJSONObject(pos);

        String postURL = "https://"+domain+"/index.php?page=post&s=view&id=" + postData.getInt("id");
        String commentURL = "https://"+domain+"/index.php?page=dapi&s=comment&q=index&post_id=" + postData.getInt("id");

        String commentsDataString = InternetCache.getData(commentURL , 60 * 60).getContent().get();

        ArrayList<Comment> comments = new ArrayList<>();
        while(commentsDataString.contains("creator=\"")) {
            String author = Tools.decryptString(Tools.cutString(commentsDataString, "creator=\"", "\""));
            String content = Tools.decryptString(Tools.cutString(commentsDataString, "body=\"", "\"")).replace("[spoiler]", "||").replace("[/spoiler]", "||");
            commentsDataString = commentsDataString.replaceFirst("creator=\"", "").replaceFirst("body=\"", "");
            comments.add(new Comment(author, content));
        }

        Instant instant;

        if (postData.has("created_at")) {
            instant = Tools.parseDateString(postData.getString("created_at"));
        } else instant = Instant.now();

        String fileURL;
        if (postData.has("file_url")) fileURL = postData.getString("file_url");
        else fileURL = imageTemplate.replace("%d", postData.getString("directory")).replace("%f", postData.getString("image"));

        if (fileURL.contains("?")) fileURL = fileURL.split("\\?")[0];


        return new PornImage(fileURL, postURL, comments, postData.getInt("score"), comments.size(), instant, !Tools.UrlContainsImage(fileURL) && !fileURL.endsWith("gif"));
    }
}
