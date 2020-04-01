package Commands;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.RandomTools;
import General.StringTools;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class EmoteAbstract extends Command implements onRecievedListener {

    private String[] gifs;
    private static HashMap<String, ArrayList<Integer>> picked = new HashMap<>();

    public EmoteAbstract() {
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<Integer> pickedCommand = picked.computeIfAbsent(getTrigger(), key -> new ArrayList<>());
        String gifUrl = gifs[RandomTools.pickFullRandom(pickedCommand, gifs.length)];
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template", "**"+event.getMessage().getAuthor().getDisplayName()+"**"))
                .setImage(gifUrl);

        event.getMessage().getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }

}
