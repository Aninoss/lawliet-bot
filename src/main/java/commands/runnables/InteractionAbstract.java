package commands.runnables;

import java.util.Locale;
import commands.Command;
import core.EmbedFactory;
import core.RandomPicker;
import core.TextManager;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class InteractionAbstract extends Command {

    private final String[] gifs;

    public InteractionAbstract(Locale locale, String prefix) {
        super(locale, prefix);
        this.gifs = getGifs();
    }

    protected abstract String[] getGifs();

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Message message = event.getMessage();
        Mention mention = MentionUtil.getMentionedString(getLocale(), message, args, event.getMember());
        boolean mentionPresent = !mention.getMentionText().isEmpty();

        if (!mentionPresent && mention.containedBlockedUser()) {
            message.getChannel().sendMessage(
                    EmbedFactory.getEmbedDefault(
                            this,
                            TextManager.getString(getLocale(), TextManager.GENERAL, "alone")
                    )
                            .setImage("https://media.discordapp.net/attachments/736277561373491265/736277600053493770/hug.gif").build())
                    .queue();
            return false;
        }

        String quote = "";
        if (mentionPresent) {
            args = mention.getFilteredOriginalText().get();
        }
        if (args.length() > 0) {
            quote = "\n\n> " + args.replace("\n", "\n> ");
        }

        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getGuild().getIdLong(), gifs.length)];
        EmbedBuilder eb;
        if (mentionPresent) {
            eb = EmbedFactory.getEmbedDefault(this, getString("template", mention.isMultiple(), mention.getMentionText(), "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote)
                    .setImage(gifUrl);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("template_single", "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote)
                    .setImage(gifUrl);
        }

        message.getChannel().sendMessage(eb.build()).queue();
        return true;
    }

}
