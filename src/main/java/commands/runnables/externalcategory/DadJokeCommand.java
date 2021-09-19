package commands.runnables.externalcategory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.Language;
import core.*;
import core.internet.HttpRequest;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.json.JSONObject;

@CommandProperties(
        trigger = "dadjoke",
        emoji = "\uD83D\uDE44",
        executableWithoutArgs = true
)
public class DadJokeCommand extends Command {

    public DadJokeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws IOException, ExecutionException, InterruptedException {
        String joke;

        if (Language.from(getLocale()) == Language.DE) {
            /* taken from https://github.com/derphilipp/Flachwitze */
            List<String> jokeList = FileManager.readInList(new LocalFile(LocalFile.Directory.RESOURCES, "dadjokes_" + getLocale().getDisplayName() + ".txt"));
            int n = RandomPicker.pick(getTrigger(), event.getGuild().getIdLong(), jokeList.size()).get();
            joke = jokeList.get(n);
        } else {
            addLoadingReactionInstantly();
            joke = new JSONObject(HttpRequest.get("https://icanhazdadjoke.com/slack").get().getBody())
                    .getJSONArray("attachments")
                    .getJSONObject(0)
                    .getString("text");
        }

        drawMessageNew(EmbedFactory.getEmbedDefault(this, joke)).exceptionally(ExceptionLogger.get());
        return true;
    }

}
