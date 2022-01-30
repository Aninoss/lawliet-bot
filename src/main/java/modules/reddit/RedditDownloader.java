package modules.reddit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import constants.RegexPatterns;
import core.MainLogger;
import core.restclient.RestClient;
import modules.PostBundle;

public class RedditDownloader {

    public CompletableFuture<Optional<RedditPost>> retrievePost(long guildId, String input, boolean nsfwAllowed) {
        String[] inputExt = extractSubredditAndOrderBy(input);
        if (inputExt != null) {
            return RestClient.WEBCACHE.get("reddit/single/" + guildId + "/" + nsfwAllowed + "/" + inputExt[0] + "/" + inputExt[1])
                    .thenApply(response -> {
                        String content = response.getBody();
                        if (content.startsWith("{")) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                mapper.registerModule(new JavaTimeModule());
                                RedditPost redditPost = mapper.readValue(content, RedditPost.class);
                                return Optional.of(redditPost);
                            } catch (JsonProcessingException e) {
                                MainLogger.get().error("Reddit post parsing error", e);
                                return Optional.empty();
                            }
                        } else {
                            return Optional.empty();
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public CompletableFuture<Optional<PostBundle<RedditPost>>> retrievePostsBulk(String input, String args) {
        String[] inputExt = extractSubredditAndOrderBy(input);
        if (inputExt != null) {
            return RestClient.WEBCACHE.get("reddit/bulk/" + inputExt[0] + "/" + inputExt[1])
                    .thenApply(response -> {
                        String content = response.getBody();
                        if (content.startsWith("[")) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                mapper.registerModule(new JavaTimeModule());
                                List<RedditPost> redditPosts = mapper.readerForListOf(RedditPost.class)
                                        .readValue(content);
                                if (redditPosts.size() > 0) {
                                    return Optional.of(processPostsBulk(redditPosts, args));
                                } else {
                                    return Optional.empty();
                                }
                            } catch (JsonProcessingException e) {
                                MainLogger.get().error("Reddit post list parsing error", e);
                                return Optional.empty();
                            }
                        } else {
                            return Optional.empty();
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private PostBundle<RedditPost> processPostsBulk(List<RedditPost> redditPosts, String args) {
        ArrayList<RedditPost> newRedditPosts = new ArrayList<>();
        ArrayList<String> usedIdList = new ArrayList<>();
        if (args != null) {
            usedIdList.addAll(Arrays.asList(args.split("\\|")));
        }

        for (RedditPost redditPost : redditPosts) {
            String id = redditPost.getId();
            if (!usedIdList.contains(id)) {
                newRedditPosts.add(redditPost);
                usedIdList.add(id);
            }
        }

        while (usedIdList.size() > 100) {
            usedIdList.remove(0);
        }

        StringBuilder newArg = new StringBuilder();
        for (int i = 0; i < usedIdList.size(); i++) {
            if (i > 0) {
                newArg.append("|");
            }
            newArg.append(usedIdList.get(i));
        }

        return new PostBundle<>(newRedditPosts, newArg.toString());
    }

    private String[] extractSubredditAndOrderBy(String input) {
        Matcher matcher = RegexPatterns.SUBREDDIT.matcher(input.replace(" ", "_"));
        if (matcher.matches()) {
            String subreddit = matcher.group("subreddit");
            String orderBy = matcher.group("orderby");
            if (orderBy == null) {
                orderBy = "hot";
            }
            return new String[] { subreddit, orderBy };
        } else {
            return null;
        }
    }

}
