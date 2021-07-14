package modules.porn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import constants.Settings;
import core.GlobalThreadPool;
import core.restclient.RestClient;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

public class BooruImageDownloader {

    private final RestClient restClient;

    public BooruImageDownloader() {
        restClient = RestClient.WEBCACHE;
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
            Invocation.Builder invocationBuilder = restClient.request("booru", MediaType.APPLICATION_JSON);
            try (Response response = invocationBuilder.post(Entity.entity(request, MediaType.APPLICATION_JSON))) {
                BooruImage booruImage = response.readEntity(BooruImage.class);
                future.complete(Optional.ofNullable(booruImage));
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

}
