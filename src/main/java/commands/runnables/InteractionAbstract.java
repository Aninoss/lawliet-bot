package commands.runnables;


import commands.Command;
import core.EmbedFactory;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.RandomPicker;
import core.TextManager;
import core.utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import java.util.Locale;

public abstract class InteractionAbstract extends Command {

    private final String[] gifs;

    public InteractionAbstract(Locale locale, String prefix) {
        super(locale, prefix);
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        Mention mention = MentionUtil.getMentionedString(getLocale(), message, followedString, event.getMessage().getAuthor().asUser().get());
        boolean mentionPresent = !mention.getMentionText().isEmpty();

        if (!mentionPresent && mention.containedBlockedUser()) {
            message.getChannel().sendMessage(
                    EmbedFactory.getEmbedDefault(this,
                            TextManager.getString(getLocale(),TextManager.GENERAL,"alone"))
                            .setImage("https://media.discordapp.net/attachments/736277561373491265/736277600053493770/hug.gif")).get();
            return false;
        }

        String quote = "";
        if (mentionPresent)
            followedString = mention.getFilteredOriginalText().get();
        if (followedString.length() > 0)
            quote = "\n\n> " + followedString.replace("\n", "\n> ");

        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getServer().get().getId(), gifs.length)];
        EmbedBuilder eb;
        if (mentionPresent) {
            eb = EmbedFactory.getEmbedDefault(this,getString("template", mention.isMultiple(), mention.getMentionText(), "**" + StringUtil.escapeMarkdown(event.getMessage().getAuthor().getDisplayName()) + "**") + quote)
                    .setImage(gifUrl);
        } else {
            eb = EmbedFactory.getEmbedDefault(this,getString("template_single", "**" + StringUtil.escapeMarkdown(event.getMessage().getAuthor().getDisplayName()) + "**") + quote)
                    .setImage(gifUrl);
        }

        message.getChannel().sendMessage(eb).get();
        return true;
    }

}
