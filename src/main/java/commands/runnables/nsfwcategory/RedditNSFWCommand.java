package commands.runnables.nsfwcategory;

import commands.listeners.CommandProperties;
import commands.runnables.PornSearchAbstract;
import modules.porn.BooruImage;
import modules.reddit.RedditDownloader;
import net.dv8tion.jda.api.components.buttons.Button;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static commands.runnables.informationcategory.HelpCommand.NSFW_SUBCATEGORY_SEARCH;

@CommandProperties(
        trigger = "redditnsfw",
        executableWithoutArgs = true,
        emoji = "\uD83D\uDD1E",
        nsfw = true,
        maxCalculationTimeSec = 5 * 60,
        requiresEmbeds = false,
        patreonRequired = true,
        subCategory = NSFW_SUBCATEGORY_SEARCH
)
public class RedditNSFWCommand extends PornSearchAbstract {

    public RedditNSFWCommand(Locale locale, String prefix) {
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
    public List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount, ArrayList<String> usedResults, boolean canBeVideo, boolean bulkMode, boolean skipAI) throws IOException {
        return super.getBooruImages(guildId, nsfwFilters, search.replaceAll("(?i)r/", ""), amount, usedResults, canBeVideo, bulkMode, skipAI);
    }

    @Override
    protected List<BooruImage> downloadPorn(long guildId, Set<String> nsfwFilter, int amount, String domain,
                                            String search, boolean animatedOnly, boolean mustBeExplicit, boolean canBeVideo,
                                            boolean bulkMode, ArrayList<String> usedResults, boolean skipAI
    ) throws IOException {
        return RedditDownloader.retrieveBooruImages(guildId, nsfwFilter, amount, search, canBeVideo, bulkMode, skipAI);
    }

    @Override
    protected Button generateReportButton(List<BooruImage> pornImages) {
        return null;
    }

}
