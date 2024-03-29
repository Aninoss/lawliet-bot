package modules.porn;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import constants.Settings;
import core.MainLogger;
import core.restclient.RestClient;

public class BooruImageDownloader {

    public CompletableFuture<Optional<BooruImage>> getPicture(long guildId, String domain, String searchTerm,
                                                              boolean animatedOnly, boolean mustBeExplicit,
                                                              boolean canBeVideo, Set<String> filters,
                                                              List<String> skippedResults, boolean test
    ) throws ExecutionException, JsonProcessingException {
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
                .setTest(test);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.post("booru", "application/json", mapper.writeValueAsString(booruRequest))
                .thenApply(response -> {
                    if (response.getCode() / 100 == 5) {
                        throw new CompletionException(new IOException("Booru retrieval error"));
                    }

                    String content = response.getBody();
                    if (!content.startsWith("{")) {
                        return Optional.empty();
                    }

                    if (test) {
                        return Optional.of(new BooruImage());
                    }

                    try {
                        BooruImage booruImage = mapper.readValue(content, BooruImage.class);
                        return Optional.of(booruImage);
                    } catch (JsonProcessingException e) {
                        MainLogger.get().error("Booru image parsing error", e);
                        return Optional.empty();
                    }
                });
    }

}
