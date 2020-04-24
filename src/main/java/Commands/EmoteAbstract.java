package Commands;


import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Utils.RandomUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class EmoteAbstract extends Command {

    private String[] gifs;
    private static final HashMap<String, ArrayList<Integer>> picked = new HashMap<>();

    public EmoteAbstract() {
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        ArrayList<Integer> pickedCommand = picked.computeIfAbsent(getTrigger(), key -> new ArrayList<>());
        String gifUrl = gifs[RandomUtil.pickFullRandom(pickedCommand, gifs.length)];
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template", "**"+event.getMessage().getAuthor().getDisplayName()+"**"))
                .setImage(gifUrl);

        event.getMessage().getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }

}
