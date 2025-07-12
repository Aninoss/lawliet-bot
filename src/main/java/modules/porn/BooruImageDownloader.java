package modules.porn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import constants.Settings;
import core.MainLogger;
import core.cache.ServerPatreonBoostCache;
import core.restclient.RestClient;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

public class BooruImageDownloader {

    public CompletableFuture<List<BooruImage>> getImages(long guildId, String domain, String searchTerm,
                                                         boolean animatedOnly, boolean mustBeExplicit,
                                                         boolean canBeVideo, boolean bulkMode, Set<String> filters,
                                                         List<String> skippedResults, int number
    ) throws JsonProcessingException {
        filters = new HashSet<>(filters);
        filters.addAll(Arrays.asList(Settings.NSFW_FILTERS));

        BooruRequest booruRequest = new BooruRequest()
                .setGuildId(guildId)
                .setDomain(domain)
                .setSearchTerm(searchTerm)
                .setAnimatedOnly(animatedOnly)
                .setMustBeExplicit(mustBeExplicit)
                .setCanBeVideo(canBeVideo)
                .setFilters(List.copyOf(filters))
                .setStrictFilters(List.of(Settings.NSFW_STRICT_FILTERS))
                .setSkippedResults(skippedResults)
                .setTest(false)
                .setPremium(ServerPatreonBoostCache.get(guildId))
                .setNumber(number)
                .setBulkMode(bulkMode);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.getClient(domain + ":" + searchTerm + ":" + canBeVideo + ":" + filters.hashCode()).post("booru_v2", "application/json", mapper.writeValueAsString(booruRequest))
                .thenApply(response -> {
                    if (response.getCode() / 100 == 5) {
                        throw new CompletionException(new IOException("Booru retrieval error"));
                    }

                    String content = response.getBody();
                    if (!content.startsWith("[")) {
                        return Collections.emptyList();
                    }

                    try {
                        ObjectReader reader = mapper.readerForListOf(BooruImage.class);
                        List<BooruImage> booruImages = reader.readValue(content);
                        booruImages.forEach(booruImage -> {
                            BooruTagCache.addTags(booruImage.getImageUrl(), booruImage.getImageTags());
                            BooruTagCache.addTags(booruImage.getPageUrl(), booruImage.getImageTags());
                        });
                        return booruImages;
                    } catch (JsonProcessingException e) {
                        MainLogger.get().error("Booru image parsing error", e);
                        return Collections.emptyList();
                    }
                });
    }

}
