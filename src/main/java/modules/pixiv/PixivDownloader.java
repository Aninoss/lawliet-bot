package modules.pixiv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import constants.Settings;
import core.MainLogger;
import core.restclient.RestClient;
import modules.PostBundle;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class PixivDownloader {

    public CompletableFuture<Optional<PixivImage>> retrieveImage(long guildId, String word, boolean nsfwAllowed,
                                                                 HashSet<String> filterSet
    ) throws JsonProcessingException {
        PixivRequest pixivRequest = new PixivRequest()
                .setGuildId(guildId)
                .setWord(word)
                .setNsfwAllowed(nsfwAllowed)
                .setFilters(List.copyOf(filterSet))
                .setStrictFilters(List.of(Settings.NSFW_STRICT_FILTERS));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.getClient(word).post("pixiv_single", "application/json", mapper.writeValueAsString(pixivRequest))
                .thenApply(response -> {
                    if (response.getCode() / 100 == 5) {
                        throw new CompletionException(new IOException("Pixiv retrieval error"));
                    }

                    String content = response.getBody();
                    if (!content.startsWith("{")) {
                        return Optional.empty();
                    }

                    try {
                        PixivImage pixivImage = mapper.readValue(content, PixivImage.class);
                        return Optional.of(pixivImage);
                    } catch (JsonProcessingException e) {
                        MainLogger.get().error("Pixiv image parsing error", e);
                        return Optional.empty();
                    }
                });
    }

    public CompletableFuture<Optional<PostBundle<PixivImage>>> retrieveImagesBulk(String word, String args, HashSet<String> filterSet) throws JsonProcessingException {
        PixivRequest pixivRequest = new PixivRequest()
                .setWord(word)
                .setFilters(List.copyOf(filterSet))
                .setStrictFilters(List.of(Settings.NSFW_STRICT_FILTERS));

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.getClient(word).post("pixiv_bulk", "application/json", mapper.writeValueAsString(pixivRequest))
                .thenApply(response -> {
                    if (response.getCode() / 100 == 5) {
                        throw new CompletionException(new IOException("Reddit retrieval error"));
                    }

                    String content = response.getBody();
                    if (!content.startsWith("[")) {
                        return Optional.empty();
                    }

                    try {
                        List<PixivImage> pixivImages = mapper.readerForListOf(PixivImage.class)
                                .readValue(content);
                        if (pixivImages.size() > 0) {
                            PostBundle<PixivImage> postBundle = PostBundle.create(pixivImages, args, PixivImage::getId);
                            return Optional.of(postBundle);
                        } else {
                            return Optional.empty();
                        }
                    } catch (JsonProcessingException e) {
                        throw new CompletionException(e);
                    }
                });
    }

}
