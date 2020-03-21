package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import MySQL.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public abstract class PornPredefinedAbstract extends PornAbstract implements onRecievedListener {

    protected abstract String getSearchKey();

    protected abstract String getSearchExtra();

    protected abstract boolean isGifOnly();

    protected abstract String getDomain();

    protected abstract String getImageTemplate();

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable {
        ArrayList<PornImage> pornImages = new ArrayList<>();

        search = getSearchKey();
        String searchAdd = getSearchExtra();
        boolean gifOnly = isGifOnly();
        String domain = getDomain();
        String imageTemplate = getImageTemplate();

        int tries = 3;
        for (int i = 0; i < amount; ) {
            PornImage pornImage = PornImageDownloader.getPicture(domain, search, searchAdd, imageTemplate, gifOnly, true, nsfwFilter);

            if (pornImage == null || pornImages.stream().anyMatch(pi -> pi.getImageUrl().equals(pornImage.getImageUrl()))) {
                tries--;
                if (tries <= 0) break;
            } else {
                tries = 3;
                pornImages.add(pornImage);
                i++;
            }
        }

        return pornImages;
    }

}
