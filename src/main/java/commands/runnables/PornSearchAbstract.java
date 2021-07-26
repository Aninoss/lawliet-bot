package commands.runnables;

import java.util.*;
import constants.Category;
import core.TextManager;
import modules.porn.BooruImage;
import modules.porn.IllegalBooruTagException;

public abstract class PornSearchAbstract extends PornAbstract {

    private String notice = null;

    public PornSearchAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount, ArrayList<String> usedResults) throws IllegalBooruTagException {
        if (search.isEmpty()) {
            search = "animated";
            notice = TextManager.getString(getLocale(), Category.NSFW, "porn_nokey");
        }

        switch (search.toLowerCase()) {
            case "hinata":
                search = "hyuuga_hinata";
                break;

            case "konosuba":
                search = "kono_subarashii_sekai_ni_shukufuku_wo!";
                break;
        }

        nsfwFilters = new HashSet<>(nsfwFilters);
        nsfwFilters.addAll(getAdditionalFilters());

        return downloadPorn(guildId, nsfwFilters, amount, getDomain(), search, false, isExplicit(),
                usedResults);
    }

    public boolean trackerUsesKey() {
        return true;
    }

}
