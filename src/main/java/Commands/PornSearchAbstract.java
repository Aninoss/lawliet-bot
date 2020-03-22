package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import MySQL.DBServer;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public abstract class PornSearchAbstract extends PornAbstract implements onRecievedListener {

    protected abstract String getDomain();
    protected abstract String getImageTemplate();

    @Override
    public ArrayList<PornImage> getPornImages(ArrayList<String> nsfwFilter, String search, int amount) throws Throwable {
        String searchAdd = Tools.getNSFWTagRemoveList(nsfwFilter);
        ArrayList<PornImage> pornImages = new ArrayList<>();

        String domain = getDomain();
        String imageTemplate = getImageTemplate();

        if (search.length() == 0) search = "animated";

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
