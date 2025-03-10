package commands.runnables;

import commands.Category;
import core.TextManager;
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
                                           ArrayList<String> usedResults, boolean canBeVideo, boolean bulkMode
    ) throws IOException {
        if (!search.isEmpty()) {
            setNotice(TextManager.getString(getLocale(), Category.NSFW, "porn_keyforbidden"));
        }

        nsfwFilters = new HashSet<>(nsfwFilters);
        nsfwFilters.addAll(getAdditionalFilters());

        String searchKey = getSearchKey();
        if (!bulkMode && isAnimatedOnly()) {
            searchKey = "animated " + searchKey;
        }

        return downloadPorn(guildId, nsfwFilters, amount, getDomain(), searchKey, isAnimatedOnly() && !bulkMode, mustBeExplicit(),
                canBeVideo, bulkMode, usedResults);
    }

    public boolean trackerUsesKey() {
        return false;
    }

}
