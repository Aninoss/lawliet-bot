package commands.runnables;

import java.util.*;
import commands.Category;
import core.TextManager;
import modules.porn.BooruImage;
import modules.porn.IllegalTagException;

public abstract class PornPredefinedAbstract extends PornAbstract {

    public PornPredefinedAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected abstract String getSearchKey();

    protected abstract boolean isAnimatedOnly();

    @Override
    public List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount,
                                           ArrayList<String> usedResults, boolean canBeVideo
    ) throws IllegalTagException {
        if (!search.isEmpty()) {
            setNotice(TextManager.getString(getLocale(), Category.NSFW, "porn_keyforbidden"));
        }

        nsfwFilters = new HashSet<>(nsfwFilters);
        nsfwFilters.addAll(getAdditionalFilters());

        return downloadPorn(guildId, nsfwFilters, amount, getDomain(), getSearchKey(), isAnimatedOnly(), mustBeExplicit(),
                canBeVideo, usedResults);
    }

    public boolean trackerUsesKey() {
        return false;
    }

}
