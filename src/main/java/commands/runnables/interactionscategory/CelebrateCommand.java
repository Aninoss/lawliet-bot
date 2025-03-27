package commands.runnables.interactionscategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.mention.Mention;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "celebrate",
        emoji = "\uD83C\uDF89",
        exclusiveUsers = { 397209883793162240L, 381156056660967426L },
        executableWithoutArgs = true,
        requiresFullMemberCache = true
)
public class CelebrateCommand extends Command {

    protected String[] getGifs() {
        return new String[] { "https://c.tenor.com/XpOVHJWYrckAAAAC/tenor.gif",
                "https://c.tenor.com/MGgHPbbIKZkAAAAC/tenor.gif",
                "https://c.tenor.com/_QVrcKYzpioAAAAC/tenor.gif",
                "https://c.tenor.com/CNnTbAEN-VEAAAAC/tenor.gif",
                "https://c.tenor.com/mv30bEOV280AAAAd/tenor.gif"
        };
    }

    public CelebrateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        Member member0 = event.getMember();
        Mention mention = MentionUtil.getMentionedString(getLocale(), event.getGuild(), args, null, event.getRepliedMember());

        if (mention.getMentionText().isEmpty()) {
            mentionBlank(event, args, member0);
        } else {
            mentionUsed(event, mention.getFilteredArgs().orElse(""), member0, mention.getMentionText());
        }

        return true;
    }

    private void mentionUsed(CommandEvent event, String args, Member author, String mention) throws ExecutionException, InterruptedException {
        String text;
        if (args.equalsIgnoreCase("with") || args.equals("mit")) {
            text = getString("template_mention_with", StringUtil.escapeMarkdown(author.getEffectiveName()), mention);
        } else {
            if (args.isEmpty()) {
                text = getString("template_mention_notext", StringUtil.escapeMarkdown(author.getEffectiveName()), mention);
            } else {
                text = getString("template_mention_text", StringUtil.escapeMarkdown(author.getEffectiveName()), args, mention);
            }
        }
        send(event, text);
    }

    private void mentionBlank(CommandEvent event, String args, Member author) throws ExecutionException, InterruptedException {
        String text;
        if (args.isEmpty()) {
            text = getString("template_nomention_notext", StringUtil.escapeMarkdown(author.getEffectiveName()));
        } else {
            text = getString("template_nomention_text", StringUtil.escapeMarkdown(author.getEffectiveName()), args);
        }
        send(event, text);
    }

    private void send(CommandEvent event, String text) throws ExecutionException, InterruptedException {
        String[] gifs = getGifs();
        String gifUrl = gifs[RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), gifs.length).get()];

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, text)
                .setImage(gifUrl);
        drawMessageNew(eb).exceptionally(ExceptionLogger.get());
    }

}
