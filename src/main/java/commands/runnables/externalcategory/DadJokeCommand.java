package commands.runnables.externalcategory;

import java.util.List;
import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import constants.Language;
import core.EmbedFactory;
import core.FileManager;
import core.RandomPicker;
import core.ResourceHandler;
import core.internet.HttpRequest;
import core.utils.StringUtil;
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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        String joke;

        if (StringUtil.getLanguage(getLocale()) == Language.DE) {
            /* taken from https://github.com/derphilipp/Flachwitze */
            List<String> jokeList = FileManager.readInList(ResourceHandler.getFileResource("data/resources/dadjokes_" + getLocale().getDisplayName() + ".txt"));
            int n = RandomPicker.getInstance().pick(getTrigger(), event.getServer().get().getId(), jokeList.size());
            joke = jokeList.get(n);
        } else {
            joke = new JSONObject(HttpRequest.getData("https://icanhazdadjoke.com/slack").get().getContent().get()).getJSONArray("attachments").getJSONObject(0).getString("text");
        }

        event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, joke)).get();
        return true;
    }
}
