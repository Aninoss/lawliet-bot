package commands.runnables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import constants.Category;
import core.TextManager;
import modules.porn.BooruImage;
import modules.porn.IllegalBooruTagException;

public abstract class PornPredefinedAbstract extends PornAbstract {

    private String notice = null;

    public PornPredefinedAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected abstract String getSearchKey();

    protected abstract String getSearchExtra();

    protected abstract boolean isAnimatedOnly();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public List<BooruImage> getBooruImages(long guildId, ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws IllegalBooruTagException {
        if (!search.isEmpty()) notice = TextManager.getString(getLocale(), Category.NSFW, "porn_keyforbidden");

        search = getSearchKey();
        String searchAdd = getSearchExtra();
        boolean animatedOnly = isAnimatedOnly();
        String domain = getDomain();

        return downloadPorn(guildId, nsfwFilter, amount, domain, search, searchAdd, animatedOnly, isExplicit(), usedResults);
    }

    public boolean trackerUsesKey() {
        return false;
    }

}
