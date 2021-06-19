package commands.runnables.gimmickscategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.Emojis;
import core.EmbedFactory;
import core.RandomPicker;
import core.utils.RandomUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Message message = event.getMessage();
        if (args.length() > 0) {
            event.getChannel().sendMessageEmbeds(getEmbed(message, args).build()).queue();
            return true;
        } else {
            event.getChannel().sendMessageEmbeds(EmbedFactory.getEmbedError(
                    this,
                    getString("no_arg")
            ).build()).queue();
            return false;
        }
    }

    private EmbedBuilder getEmbed(Message message, String question) {
        question = StringUtil.shortenString(question, 1024);
        int n = RandomPicker.getInstance().pick(getTrigger(), message.getGuild().getIdLong(), 27);
        String answerRaw = getString("answer_" + n);

        String answer = answerRaw;
        if (answer.equals("%RandomUpperCase")) {
            answer = RandomUtil.randomUpperCase(question);
        } else if (answer.startsWith("%Gif")) {
            answer = Emojis.ZERO_WIDTH_SPACE;
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this)
                .addField(getString("question", StringUtil.escapeMarkdown(message.getMember().getEffectiveName())), question, false)
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
