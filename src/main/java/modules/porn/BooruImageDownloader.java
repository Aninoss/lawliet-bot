package modules.porn;

import java.util.*;
import java.util.concurrent.CompletableFuture;
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
                                                              boolean animatedOnly, boolean explicit,
                                                              Set<String> filters, List<String> skippedResults,
                                                              boolean test
    ) throws ExecutionException, JsonProcessingException {
        filters = new HashSet<>(filters);
        filters.addAll(Arrays.asList(Settings.NSFW_FILTERS));

        BooruRequest booruRequest = new BooruRequest()
                .setGuildId(guildId)
                .setDomain(domain)
                .setSearchTerm(searchTerm)
                .setAnimatedOnly(animatedOnly)
                .setExplicit(explicit)
                .setFilters(List.copyOf(filters))
                .setSkippedResults(skippedResults)
                .setTest(test);

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.post("booru", "application/json", mapper.writeValueAsString(booruRequest))
                .thenApply(response -> {
                    String content = response.getBody();
                    if (content.startsWith("{")) {
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
                    } else {
                        return Optional.empty();
                    }
                });
    }

}
