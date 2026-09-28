package commands.runnables;

import modules.porn.BooruImage;
import modules.reddit.RedditDownloader;
import net.dv8tion.jda.api.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class RedditNSFWAbstract extends PornPredefinedAbstract {

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
                                            boolean bulkMode, ArrayList<String> usedResults, boolean skipAI
    ) throws IOException {
        return RedditDownloader.retrieveBooruImages(guildId, nsfwFilter, amount, getSearchKey(), canBeVideo, bulkMode, skipAI);
    }

    @Override
    protected boolean isAnimatedOnly() {
        return false;
    }

    @Override
    protected Button generateReportButton(List<BooruImage> pornImages) {
        return null;
    }

}
