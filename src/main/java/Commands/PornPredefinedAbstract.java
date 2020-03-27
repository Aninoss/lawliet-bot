package Commands;

import CommandListeners.onRecievedListener;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import General.TextManager;

import java.util.ArrayList;
import java.util.Optional;

public abstract class PornPredefinedAbstract extends PornAbstract implements onRecievedListener {

    private String notice = null;

    protected abstract String getSearchKey();
    protected abstract String getSearchExtra();
    protected abstract boolean isAnimatedOnly();
    protected abstract String getDomain();
    protected abstract String getImageTemplate();

    @Override
    public Optional<String> getNoticeOptional() {
        return Optional.ofNullable(notice);
    }

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable {
        ArrayList<PornImage> pornImages = new ArrayList<>();

        if (!search.isEmpty()) notice = TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_keyforbidden");

        search = getSearchKey();
        String searchAdd = getSearchExtra();
        boolean animatedOnly = isAnimatedOnly();
        String domain = getDomain();
        String imageTemplate = getImageTemplate();

        int tries = 5;
        for (int i = 0; i < amount; ) {
            PornImage pornImage = PornImageDownloader.getPicture(domain, search, searchAdd, imageTemplate, animatedOnly, true, nsfwFilter);

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
