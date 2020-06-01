package Commands;


import CommandSupporters.Command;
import Core.*;
import Core.Mention.Mention;
import Core.Mention.MentionUtil;
import Core.Utils.RandomUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class InteractionAbstract extends Command {

    private final String[] gifs;

    public InteractionAbstract() { this.gifs = getGifs(); }

    protected abstract String[] getGifs();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        Mention mention = MentionUtil.getMentionedString(getLocale(), message, followedString);
        if (mention != null && mention.toString().equals( "**"+event.getMessage().getAuthor().getDisplayName()+"**")) {
            message.getChannel().sendMessage(
                    EmbedFactory.getCommandEmbedStandard(this,
                        TextManager.getString(getLocale(),TextManager.GENERAL,"alone"))
                    .setImage("https://media1.giphy.com/media/od5H3PmEG5EVq/giphy.gif?cid=790b76115ce968cf4a364a6845982172&rid=giphy.gif")).get();
            return false;
        }

        String quote = "";
        if (mention != null)
            followedString = mention.getFilteredOriginalText().get();
        if (followedString.length() > 0)
            quote = "\n\n> " + followedString.replace("\n", "\n> ");

        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getServer().get().getId(), gifs.length)];
        EmbedBuilder eb;
        if (mention != null) {
            eb = EmbedFactory.getCommandEmbedStandard(this,getString("template", mention.isMultiple(), mention.toString(), "**"+event.getMessage().getAuthor().getDisplayName()+"**") + quote)
                    .setImage(gifUrl);
        } else {
            eb = EmbedFactory.getCommandEmbedStandard(this,getString("template_single", "**"+event.getMessage().getAuthor().getDisplayName()+"**") + quote)
                    .setImage(gifUrl);
        }

        message.getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }

}
