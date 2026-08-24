package modules.reddit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import core.MainLogger;
import core.restclient.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RedditAutoComplete {

    public CompletableFuture<List<SubredditAutoComplete>> getAutoComplete(String query) {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.getClient(query).get("reddit_autocomplete/" + encodedQuery)
                .thenApply(response -> {
                    String content = response.getBody();
                    if (content.startsWith("[")) {
                        try {
                            ObjectReader reader = mapper.readerForListOf(SubredditAutoComplete.class);
                            return reader.readValue(content);
                        } catch (JsonProcessingException e) {
                            MainLogger.get().error("Subreddit auto complete list parsing error", e);
                            return Collections.emptyList();
                        }
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

}
