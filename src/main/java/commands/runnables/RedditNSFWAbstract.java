package commands.runnables;

import core.utils.InternetUtil;
import modules.porn.BooruImage;
import modules.reddit.RedditDownloader;
import modules.reddit.RedditPost;
import net.dv8tion.jda.api.components.buttons.Button;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class RedditNSFWAbstract extends PornPredefinedAbstract {

    private static final RedditDownloader redditDownloader = new RedditDownloader();

    public RedditNSFWAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public String getDomain() {
        return "reddit.com";
    }

    @Override
    public boolean mustBeExplicit() {
        return true;
    }

    @Override
    protected List<BooruImage> downloadPorn(long guildId, Set<String> nsfwFilter, int amount, String domain,
                                            String search, boolean animatedOnly, boolean mustBeExplicit, boolean canBeVideo,
                                            boolean bulkMode, ArrayList<String> usedResults
    ) throws IOException {
        try {
            if (bulkMode) {
                return redditDownloader.retrievePostsBulk(getSearchKey()).get().stream()
                        .map(redditPost -> mapToBooruImage(redditPost, canBeVideo))
                        .collect(Collectors.toList());
            } else {
                ArrayList<BooruImage> images = new ArrayList<>();
                for (int i = 0; i < amount; i++) {
                    redditDownloader.retrievePost(guildId, getSearchKey(), mustBeExplicit).get()
                            .ifPresent(redditPost -> images.add(mapToBooruImage(redditPost, canBeVideo)));
                }
                return images;
            }
        } catch (Throwable e) {
            throw new IOException("Reddit retrieval error");
        }
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

    @Override
    protected Button generateReportButton(List<BooruImage> pornImages) {
        return null;
    }

    private BooruImage mapToBooruImage(RedditPost redditPost, boolean canBeVideo) {
        if (redditPost.getImage() == null || redditPost.getImage().isEmpty()) {
            redditPost.setImage(redditPost.getThumbnail());
        }
        return new BooruImage()
                .setId(redditPost.getId().hashCode())
                .setImageUrl(!InternetUtil.uriIsVideo(redditPost.getImage()) || canBeVideo || redditPost.getThumbnail() == null || redditPost.getThumbnail().isBlank() ? redditPost.getImage() : redditPost.getThumbnail())
                .setOriginalImageUrl(redditPost.getImage())
                .setPageUrl(redditPost.getSourceLink() != null ?  redditPost.getSourceLink() : redditPost.getUrl())
                .setScore(redditPost.getScore())
                .setInstant(redditPost.getInstant())
                .setVideo(InternetUtil.uriIsVideo(redditPost.getImage()))
                .setTags(Collections.emptyList())
                .setImageTags(Collections.emptyList());
    }

}
