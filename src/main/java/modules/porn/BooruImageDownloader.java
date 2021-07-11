package modules.porn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import constants.Settings;
import core.GlobalThreadPool;
import core.RestClient;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

public class BooruImageDownloader {

    private final RestClient restClient;

    public BooruImageDownloader() {
        restClient = new RestClient(
                System.getenv("WEBCACHE_HOST"),
                Integer.parseInt(System.getenv("WEBCACHE_PORT")),
                "api/",
                System.getenv("WEBCACHE_AUTH")
        );
    }

    public CompletableFuture<Optional<BooruImage>> getPicture(long guildId, String domain, String searchTerm, String searchTermExtra,
                                                              String imageTemplate, boolean animatedOnly, boolean canBeVideo,
                                                              boolean explicit, List<String> filters, List<String> skippedResults
    ) throws ExecutionException {
        ArrayList<String> newFilters = new ArrayList<>(filters);
        newFilters.addAll(Arrays.asList(Settings.NSFW_FILTERS));

        BooruRequest request = new BooruRequest()
                .setGuildId(guildId)
                .setDomain(domain)
                .setSearchTerm(searchTerm)
                .setSearchTermExtra(searchTermExtra)
                .setImageTemplate(imageTemplate)
                .setAnimatedOnly(animatedOnly)
                .setCanBeVideo(canBeVideo)
                .setExplicit(explicit)
                .setFilters(newFilters)
                .setSkippedResults(skippedResults);

        CompletableFuture<Optional<BooruImage>> future = new CompletableFuture<>();
        GlobalThreadPool.getExecutorService().submit(() -> {
            try {
                BooruImage booruImage = restClient.request("booru", MediaType.APPLICATION_JSON)
                        .post(Entity.entity(request, MediaType.APPLICATION_JSON))
                        .readEntity(BooruImage.class);
                future.complete(Optional.ofNullable(booruImage));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

}
