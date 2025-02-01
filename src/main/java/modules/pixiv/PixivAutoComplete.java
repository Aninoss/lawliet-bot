package modules.pixiv;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import core.MainLogger;
import core.restclient.RestClient;
import core.utils.NSFWUtil;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PixivAutoComplete {

    public CompletableFuture<List<PixivChoice>> getTags(String search, HashSet<String> nsfwAdditionalFilters) {
        String encodedSearch = URLEncoder.encode(search, StandardCharsets.UTF_8);
        if (encodedSearch.isEmpty()) {
            encodedSearch = "+";
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return RestClient.WEBCACHE.getClient(encodedSearch).get("pixiv_autocomplete/" + encodedSearch + "/")
                .thenApply(response -> {
                    String content = response.getBody();
                    if (content.startsWith("[")) {
                        try {
                            ObjectReader reader = mapper.readerForListOf(PixivChoice.class);
                            List<PixivChoice> choices = reader.readValue(content);
                            return choices.stream()
                                    .filter(ch -> !NSFWUtil.containsFilterTags(NSFWUtil.expandTags(ch.getTag()), nsfwAdditionalFilters) &&
                                            (ch.getTranslatedTag() == null || !NSFWUtil.containsFilterTags(NSFWUtil.expandTags(ch.getTranslatedTag()), nsfwAdditionalFilters)))
                                    .collect(Collectors.toList());
                        } catch (JsonProcessingException e) {
                            MainLogger.get().error("Pixiv choices list parsing error", e);
                            return Collections.emptyList();
                        }
                    } else {
                        return Collections.emptyList();
                    }
                });
    }

}
