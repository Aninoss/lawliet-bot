package commands.runnables.interactionscategory;

import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.RandomPicker;
import core.mention.Mention;
import core.utils.MentionUtil;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "celebrate",
    emoji = "\uD83C\uDF89",
    exclusiveUsers = { 397209883793162240L, 381156056660967426L },
    executableWithoutArgs = true
)
public class CelebrateCommand extends Command {

    protected String[] getGifs() {
        return new String[]{"https://media1.tenor.com/images/53e00327a221637f76bdb3d20e4568a0/tenor.gif?itemid=7399759",
                "https://media1.tenor.com/images/fbbd906d9cb5624fbafd7f536aec5cc3/tenor.gif?itemid=16786818",
                "https://media1.tenor.com/images/3d2574a66760415d655fdd6f5e57c044/tenor.gif?itemid=18653960",
                "https://media1.tenor.com/images/07869599e4a13f14ed4a6425c835537a/tenor.gif?itemid=15417564",
                "https://media1.tenor.com/images/ade6ea654ec8e7c6de665d9c58836455/tenor.gif?itemid=16271642"
        };
    }

    public CelebrateCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        User user0 = event.getMessage().getUserAuthor().get();
        Mention mention = MentionUtil.getMentionedString(getLocale(), event.getMessage(), args, null);

        if (mention.getMentionText().isEmpty())
            mentionBlank(event, args, user0);
        else
            mentionUsed(event, mention.getFilteredOriginalText().orElse(""), user0, mention.getMentionText());

        return true;
    }

    private void mentionUsed(MessageCreateEvent event, String args, User author, String mention) throws ExecutionException, InterruptedException {
        String text;
        if (args.equalsIgnoreCase("with") || args.equals("mit")) {
            text = getString("template_mention_with", author.getDisplayName(event.getServer().get()), mention);
        } else {
            if (args.isEmpty())
                text = getString("template_mention_notext", author.getDisplayName(event.getServer().get()), mention);
            else
                text = getString("template_mention_text", author.getDisplayName(event.getServer().get()), args, mention);
        }
        send(event, text);
    }

    private void mentionBlank(MessageCreateEvent event, String args, User author) throws ExecutionException, InterruptedException {
        String text;
        if (args.isEmpty())
            text = getString("template_nomention_notext", author.getDisplayName(event.getServer().get()));
        else
            text = getString("template_nomention_text", author.getDisplayName(event.getServer().get()), args);
        send(event, text);
    }

    private void send(MessageCreateEvent event, String text) throws ExecutionException, InterruptedException {
        String[] gifs = getGifs();
        String gifUrl = gifs[RandomPicker.getInstance().pick(getTrigger(), event.getGuild().getIdLong(), gifs.length)];

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, text)
                .setImage(gifUrl);
        event.getChannel().sendMessage(eb).get();
    }

}
