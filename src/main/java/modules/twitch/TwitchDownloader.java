package modules.twitch;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import constants.RegexPatterns;
import core.restclient.RestClient;
import core.utils.InternetUtil;

public class TwitchDownloader {

    public CompletableFuture<Optional<TwitchStream>> retrieveStream(String name) {
        if (RegexPatterns.TWITCH.matcher(name).matches()) {
            return RestClient.WEBCACHE.get("twitch/" + InternetUtil.escapeForURL(name.toLowerCase()))
                    .thenApply(response -> {
                        String content = response.getBody();
                        if (content.startsWith("{")) {
                            try {
                                ObjectMapper mapper = new ObjectMapper();
                                mapper.registerModule(new JavaTimeModule());
                                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                                TwitchStream twitchStream = mapper.readValue(content, TwitchStream.class);
                                return Optional.of(twitchStream);
                            } catch (JsonProcessingException e) {
                                throw new RuntimeException(e);
                            }
                        } else if (content.isEmpty()) {
                            return Optional.empty();
                        } else {
                            throw new RuntimeException("Error");
                        }
                    });
        } else {
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

}
