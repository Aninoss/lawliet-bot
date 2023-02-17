package commands.runnables;

import java.io.IOException;
import java.util.*;
import commands.Category;
import core.TextManager;
import modules.porn.BooruImage;
import modules.porn.TooManyTagsException;

public abstract class PornSearchAbstract extends PornAbstract {

    public PornSearchAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    public int getMaxTags() {
        return -1;
    }

    @Override
    public List<BooruImage> getBooruImages(long guildId, Set<String> nsfwFilters, String search, int amount,
                                           ArrayList<String> usedResults, boolean canBeVideo
    ) throws IOException {
        if (search.isEmpty()) {
            search = "animated";
            setNotice(TextManager.getString(getLocale(), Category.NSFW, "porn_nokey"));
        }

        switch (search.toLowerCase()) {
            case "hinata" -> search = "hyuuga_hinata";
            case "hanabi" -> search = "hyuuga_hanabi";
            case "konosuba" -> search = "kono_subarashii_sekai_ni_shukufuku_wo!";
            case "dxd", "highschool_dxd" -> search = "high_school_dxd";
            case "zerotwo", "zero_two", "zero two" -> search = "zero_two_(darling_in_the_franxx)";
            case "rem" -> search = "rem_(re:zero)";
            case "brawl star" -> search = "brawl_stars";
            case "brawl stars" -> search = "brawl_stars";
        }

        int maxTags = getMaxTags();
        int tags = countTags(search);
        if (maxTags != -1 && tags > maxTags) {
            throw new TooManyTagsException(maxTags);
        }

        nsfwFilters = new HashSet<>(nsfwFilters);
        nsfwFilters.addAll(getAdditionalFilters());

        return downloadPorn(guildId, nsfwFilters, amount, getDomain(), search, false, mustBeExplicit(),
                canBeVideo, usedResults);
    }

    @Override
    public boolean trackerUsesKey() {
        return true;
    }

    private int countTags(String search) {
        return search.replace("+", " ").split(" ").length;
    }

}
