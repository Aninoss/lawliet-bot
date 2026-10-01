package modules.reddit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import constants.RegexPatterns;
import core.restclient.RestClient;
import core.utils.InternetUtil;
import core.utils.NSFWUtil;
import modules.porn.BooruImage;
import modules.porn.IllegalTagException;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class RedditDownloader {

    public static CompletableFuture<Optional<RedditPost>> retrievePost(long guildId, String input, boolean nsfwAllowed) {
        String[] inputExt = extractSubredditAndOrderBy(input);
        if (inputExt != null) {
            return RestClient.WEBCACHE.getClient(input).get("reddit/single/" + guildId + "/" + nsfwAllowed + "/" + inputExt[0] + "/" + inputExt[1])
                    .thenApply(response -> {
                        if (response.getCode() / 100 == 5) {
                            throw new CompletionException(new IOException("Reddit retrieval error"));
                        }

                        String content = response.getBody();
                        if (content.startsWith("{")) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                mapper.registerModule(new JavaTimeModule());
                                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                RedditPost redditPost = mapper.readValue(content, RedditPost.class);
                                return Optional.of(redditPost);
                            } catch (JsonProcessingException e) {
                                throw new CompletionException(e);
                            }
                        } else {
                            return Optional.empty();
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    public static CompletableFuture<List<RedditPost>> retrievePostsBulk(String input) {
        String[] inputExt = extractSubredditAndOrderBy(input);
        if (inputExt == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        return RestClient.WEBCACHE.getClient(input).get("reddit/bulk/" + inputExt[0] + "/" + inputExt[1])
                .thenApply(response -> {
                    if (response.getCode() / 100 == 5) {
                        throw new CompletionException(new IOException("Reddit retrieval error"));
                    }

                    String content = response.getBody();
                    if (!content.startsWith("[")) {
                        return List.of();
                    }

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.registerModule(new JavaTimeModule());
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        return mapper.readerForListOf(RedditPost.class)
                                .readValue(content);
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                });
    }

    public static List<BooruImage> retrieveBooruImages(long guildId, Set<String> nsfwFilter, int amount,
                                                 String search, boolean canBeVideo, boolean bulkMode, boolean skipAI
    ) throws IOException {
        if (NSFWUtil.containsFilterTags(search, nsfwFilter, skipAI)) {
            throw new IllegalTagException();
        }

        try {
            if (bulkMode) {
                return retrievePostsBulk(search).get().stream()
                        .map(redditPost -> mapToBooruImage(redditPost, canBeVideo))
                        .collect(Collectors.toList());
            } else {
                ArrayList<BooruImage> images = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    retrievePost(guildId, search, true).get()
                            .ifPresent(redditPost -> images.add(mapToBooruImage(redditPost, canBeVideo)));
                }
                return images;
            }
        } catch (Throwable e) {
            throw new IOException("Reddit retrieval error");
        }
    }

    private static String[] extractSubredditAndOrderBy(String input) {
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

    private static BooruImage mapToBooruImage(RedditPost redditPost, boolean canBeVideo) {
        if (redditPost.getMediaUrls() == null || redditPost.getMediaUrls().isEmpty()) {
            redditPost.setMediaUrls(List.of(redditPost.getThumbnail()));
        }
        String mediaUrl = redditPost.getMediaUrls().isEmpty() ? null : redditPost.getMediaUrls().get(0);
        return new BooruImage()
                .setId(redditPost.getId().hashCode())
                .setImageUrl(!InternetUtil.uriIsVideo(mediaUrl) || canBeVideo || redditPost.getThumbnail() == null || redditPost.getThumbnail().isBlank() ? mediaUrl : redditPost.getThumbnail())
                .setPageUrl(redditPost.getRedditUrl())
                .setScore(redditPost.getScore())
                .setInstant(redditPost.getInstant())
                .setVideo(InternetUtil.uriIsVideo(mediaUrl))
                .setImageTags(Collections.emptyList())
                .setApproximateResults(false);
    }

}
