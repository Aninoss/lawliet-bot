package commands.runnables;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
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

public abstract class RolePlayAbstract extends Command {

    private final boolean interactive;
    private final String[] gifs;

    public RolePlayAbstract(Locale locale, String prefix, boolean interactive, String... gifs) {
        super(locale, prefix);
        this.interactive = interactive;
        this.gifs = gifs;
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        if (interactive) {
            return onTriggerInteractive(event, args);
        } else {
            return onTriggerNonInteractive(event, args);
        }
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean onTriggerInteractive(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        Message message = event.getMessage();
        Mention mention = MentionUtil.getMentionedString(getLocale(), message, args, event.getMember());
        boolean mentionPresent = !mention.getMentionText().isEmpty();

        if (!mentionPresent && mention.containedBlockedUser()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                    this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "alone")
            ).setImage("https://cdn.discordapp.com/attachments/736277561373491265/736277600053493770/hug.gif");
            drawMessageNew(eb);
            return false;
        }

        String quote = "";
        if (mentionPresent) {
            args = mention.getFilteredArgs().get();
        }
        if (args.length() > 0) {
            quote = "\n\n>>> " + args;
        }

        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];
        EmbedBuilder eb;
        if (mentionPresent) {
            eb = EmbedFactory.getEmbedDefault(this, getString("template", mention.isMultiple(), mention.getMentionText(), "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote)
                    .setImage(gifUrl);
        } else {
            eb = EmbedFactory.getEmbedDefault(this, getString("template_single", "**" + StringUtil.escapeMarkdown(event.getMember().getEffectiveName()) + "**") + quote)
                    .setImage(gifUrl);
        }

        drawMessageNew(eb);
        return true;
    }

    public boolean onTriggerNonInteractive(GuildMessageReceivedEvent event, String args) throws ExecutionException, InterruptedException {
        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];

        String quote = "";
        if (args.length() > 0) {
            quote = "\n\n>>> " + args;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(
                this,
                getString("template", "**" + StringUtil.escapeMarkdown(event.getMessage().getMember().getEffectiveName()) + "**") + quote
        ).setImage(gifUrl);

        drawMessageNew(eb);
        return true;
    }

}
