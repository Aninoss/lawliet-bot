package commands.runnables.externalcategory;

import commands.listeners.CommandProperties;

import commands.Command;
import constants.Language;
import core.*;
import core.internet.HttpRequest;
import core.utils.StringUtil;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONObject;
import java.util.List;
import java.util.Locale;

@CommandProperties(
    trigger = "dadjoke",
    withLoadingBar = true,
    emoji = "\uD83D\uDE44",
    executableWithoutArgs = true
)
public class DadJokeCommand extends Command {

    public DadJokeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
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
