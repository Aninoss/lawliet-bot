package Commands;


import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import General.Tools.NSFWTools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public abstract class PornSearchAbstract extends PornAbstract {

    private String notice = null;

    protected abstract String getDomain();
    protected abstract String getImageTemplate();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable {
        String searchAdd = NSFWTools.getNSFWTagRemoveList(nsfwFilter);

        String domain = getDomain();
        String imageTemplate = getImageTemplate();

        if (search.isEmpty()) {
            search = "animated";
            notice = TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_nokey");
        }

        switch (search.toLowerCase()) {
            case "hinata":
                search = "hyuuga_hinata";
                break;
            case "konosuba":
                search = "kono_subarashii_sekai_ni_shukufuku_wo!";
                break;
        }

        return downloadPorn(nsfwFilter, amount, domain, search, searchAdd, imageTemplate, false);
    }

}
