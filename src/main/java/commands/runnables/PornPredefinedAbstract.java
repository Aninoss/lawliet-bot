package commands.runnables;

import modules.porn.BooruImage;

import java.io.IOException;
import java.util.*;

public abstract class PornPredefinedAbstract extends PornAbstract {

    public PornPredefinedAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    abstract protected String getSearchKey();

    abstract protected boolean isAnimatedOnly();

    @Override
    public List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount,
                                           ArrayList<String> usedResults, boolean canBeVideo, boolean bulkMode,
                                           boolean skipAI
    ) throws IOException {
        nsfwFilters = new HashSet<>(nsfwFilters);
        nsfwFilters.addAll(getAdditionalFilters());

        String searchKey = search + " " + getSearchKey();
        if (!bulkMode && isAnimatedOnly()) {
            searchKey = "animated " + searchKey;
        }

        return downloadPorn(guildId, nsfwFilters, amount, getDomain(), searchKey.trim(), isAnimatedOnly() && !bulkMode, mustBeExplicit(),
                canBeVideo, bulkMode, usedResults, skipAI);
    }

    @Override
    public boolean trackerAllowEmptyKey() {
        return true;
    }

}
