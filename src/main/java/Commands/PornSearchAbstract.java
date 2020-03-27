package Commands;

import CommandListeners.onRecievedListener;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;

import java.util.ArrayList;
import java.util.Optional;

public abstract class PornSearchAbstract extends PornAbstract implements onRecievedListener {

    private String notice = null;

    protected abstract String getDomain();
    protected abstract String getImageTemplate();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable {
        String searchAdd = Tools.getNSFWTagRemoveList(nsfwFilter);
        ArrayList<PornImage> pornImages = new ArrayList<>();

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

        int tries = 5;
        for (int i = 0; i < amount; ) {
            PornImage pornImage = PornImageDownloader.getPicture(domain, search, searchAdd, imageTemplate, false, true, nsfwFilter);

            if (pornImage == null || pornImages.stream().anyMatch(pi -> pi.getImageUrl().equals(pornImage.getImageUrl()))) {
                tries--;
                if (tries <= 0) break;
            } else {
                tries = 5;
                pornImages.add(pornImage);
                i++;
            }
        }

        return pornImages;
    }

}
