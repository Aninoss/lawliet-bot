package commands.runnables.gimmickscategory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import constants.Emojis;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.RandomPicker;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "fortune",
        emoji = "❓",
        executableWithoutArgs = false,
        aliases = { "question", "8ball" }
)
public class FortuneCommand extends Command {

    public FortuneCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws ExecutionException, InterruptedException {
        if (args.length() > 0) {
            drawMessageNew(getEmbed(event.getMember(), args))
                    .exceptionally(ExceptionLogger.get());
            return true;
        } else {
            drawMessageNew(EmbedFactory.getEmbedError(
                    this,
                    getString("no_arg")
            )).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

    private EmbedBuilder getEmbed(Member member, String question) throws ExecutionException, InterruptedException {
        question = StringUtil.shortenString(question, 1024);
        int n = RandomPicker.pick(getTrigger(), member.getGuild().getIdLong(), 27).get();
        String answerRaw = getString("answer_" + n);

        String answer = answerRaw;
        if (answer.equals("%RandomUpperCase")) {
            answer = RandomUtil.randomUpperCase(question);
        } else if (answer.startsWith("%Gif")) {
            answer = Emojis.ZERO_WIDTH_SPACE.getFormatted();
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(getString("question", StringUtil.escapeMarkdown(member.getEffectiveName())), question, false)
                .addField(getString("answer"), answer, false);

        if (answerRaw.equals("%GifNo")) {
            eb.setImage("https://cdn.discordapp.com/attachments/711665117770547223/711665289359786014/godno.jpg");
        }
        if (answerRaw.equals("%GifYes")) {
            eb.setImage("https://cdn.discordapp.com/attachments/711665117770547223/711665290601037904/yes.gif");
        }

        return eb;
    }

}
