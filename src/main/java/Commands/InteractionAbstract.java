package Commands;


import CommandSupporters.Command;
import Core.*;
import Core.Mention.Mention;
import Core.Mention.MentionTools;
import Core.Utils.RandomUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class InteractionAbstract extends Command {

    private String[] gifs;
    private static final HashMap<String, ArrayList<Integer>> picked = new HashMap<>();

    public InteractionAbstract() { this.gifs = getGifs(); }

    protected abstract String[] getGifs();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        Mention mention = MentionTools.getMentionedString(getLocale(), message, followedString);
        if (mention == null) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(),TextManager.GENERAL,"no_mentions"))).get();
            return false;
        } else if (mention.getString().equals( "**"+event.getMessage().getAuthor().getDisplayName()+"**")) {
            message.getChannel().sendMessage(
                    EmbedFactory.getCommandEmbedStandard(this,
                        TextManager.getString(getLocale(),TextManager.GENERAL,"alone"))
                    .setImage("https://media1.giphy.com/media/od5H3PmEG5EVq/giphy.gif?cid=790b76115ce968cf4a364a6845982172&rid=giphy.gif")).get();
            return false;
        }

        ArrayList<Integer> pickedCommand = picked.computeIfAbsent(getTrigger(), key -> new ArrayList<>());
        String gifUrl = gifs[RandomUtil.pickFullRandom(pickedCommand, gifs.length)];
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template", mention.isMultiple(), mention.getString(), "**"+event.getMessage().getAuthor().getDisplayName()+"**"))
                .setImage(gifUrl);

        message.getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }

}
