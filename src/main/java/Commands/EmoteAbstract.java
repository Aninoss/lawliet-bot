package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Tools;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public abstract class EmoteAbstract extends Command implements onRecievedListener {

    private String[] gifs;
    private static ArrayList<Integer> picked = new ArrayList<>();

    public EmoteAbstract() {
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        String gifUrl = gifs[Tools.pickFullRandom(picked, gifs.length)];
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template", "**"+event.getMessage().getAuthor().getDisplayName()+"**"))
                .setImage(gifUrl);

        event.getMessage().getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }

}
