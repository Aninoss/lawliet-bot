package Commands;


import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.RandomPicker;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

public abstract class EmoteAbstract extends Command {

    private final String[] gifs;

    public EmoteAbstract(Locale locale, String prefix) {
        super(locale, prefix);
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getServer().get().getId(), gifs.length)];

        String quote = "";
        if (followedString.length() > 0)
            quote = "\n\n> " + followedString.replace("\n", "\n> ");

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("template", "**"+event.getMessage().getAuthor().getDisplayName()+"**") + quote)
                .setImage(gifUrl);

        event.getMessage().getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }

}
