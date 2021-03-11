package commands.runnables;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import constants.Category;
import core.TextManager;
import modules.porn.PornImage;

public abstract class PornPredefinedAbstract extends PornAbstract {

    private String notice = null;

    public PornPredefinedAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected abstract String getSearchKey();

    protected abstract String getSearchExtra();

    protected abstract boolean isAnimatedOnly();

    protected abstract String getImageTemplate();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws Exception {
        if (!search.isEmpty()) notice = TextManager.getString(getLocale(), Category.NSFW, "porn_keyforbidden");

        search = getSearchKey();
        String searchAdd = getSearchExtra();
        boolean animatedOnly = isAnimatedOnly();
        String domain = getDomain();
        String imageTemplate = getImageTemplate();

        return downloadPorn(nsfwFilter, amount, domain, search, searchAdd, imageTemplate, animatedOnly, isExplicit(), usedResults);
    }

    public boolean trackerUsesKey() {
        return false;
    }

}
