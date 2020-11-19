package commands.runnables.gimmickscategory;

import commands.listeners.CommandProperties;
import commands.Command;
import constants.Emojis;
import core.EmbedFactory;
import core.RandomPicker;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "fortune",
        emoji = "❓",
        executableWithoutArgs = false,
        aliases = {"question", "8ball"}
)
public class FortuneCommand extends Command {

    public FortuneCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        if (followedString.length() > 0) {
            event.getChannel().sendMessage(getEmbed(message, followedString)).get();
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    getString("no_arg"))).get();
            return false;
        }
    }

    private EmbedBuilder getEmbed(Message message, String question) {
        question = StringUtil.shortenString(question, 1024);
        int n = RandomPicker.getInstance().pick(getTrigger(), message.getServer().get().getId(), 27);
        String answerRaw = getString("answer_" + n);

        String answer = answerRaw;
        if (answer.equals("%RandomUpperCase")) {
            answer = RandomUtil.randomUpperCase(question);
        } else if (answer.startsWith("%Gif")) {
            answer = Emojis.EMPTY_EMOJI;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(getString("question", StringUtil.escapeMarkdown(message.getAuthor().getDisplayName())), question)
                .addField(getString("answer"), answer);

        if (answerRaw.equals("%GifNo")) eb.setImage("https://cdn.discordapp.com/attachments/711665117770547223/711665289359786014/godno.jpg");
        if (answerRaw.equals("%GifYes")) eb.setImage("https://cdn.discordapp.com/attachments/711665117770547223/711665290601037904/yes.gif");

        return eb;
    }
}
