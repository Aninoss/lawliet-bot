package commands.runnables;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import constants.Category;
import core.TextManager;
import core.utils.NSFWUtil;
import modules.porn.BooruImage;
import modules.porn.IllegalBooruTagException;

public abstract class PornSearchAbstract extends PornAbstract {

    private String notice = null;

    public PornSearchAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    protected abstract String getImageTemplate();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public List<BooruImage> getBooruImages(long guildId, ArrayList<String> nsfwFilter, String search, int amount, ArrayList<String> usedResults) throws IllegalBooruTagException {
        String searchAdd = NSFWUtil.getNSFWTagRemoveList(nsfwFilter);

        String domain = getDomain();
        String imageTemplate = getImageTemplate();

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

        return downloadPorn(guildId, nsfwFilter, amount, domain, search, searchAdd + getAdditionalSearchKey(), imageTemplate, false, isExplicit(), usedResults);
    }

    protected String getAdditionalSearchKey() {
        return "";
    }

    public boolean trackerUsesKey() {
        return true;
    }

}
