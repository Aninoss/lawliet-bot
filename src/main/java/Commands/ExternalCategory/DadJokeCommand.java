package Commands.ExternalCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Language;
import Core.*;
import Core.Internet.HttpRequest;
import Core.Utils.StringUtil;
import org.javacord.api.event.message.MessageCreateEvent;
import org.json.JSONObject;

@CommandProperties(
    trigger = "dadjoke",
    withLoadingBar = true,
    emoji = "\uD83D\uDE44",
    thumbnail = "http://icons.iconarchive.com/icons/webalys/kameleon.pics/128/Man-6-icon.png",
    executable = true
)
public class DadJokeCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        String joke;

        if (StringUtil.getLanguage(getLocale()) == Language.DE) {
            joke = HttpRequest.getData("https://api.opossum.media/streamacademy/commands/fun/flachwitz.php").get().getContent().get().split("\\|")[0];
        } else {
            joke = new JSONObject(HttpRequest.getData("https://icanhazdadjoke.com/slack").get().getContent().get()).getJSONArray("attachments").getJSONObject(0).getString("text");
        }

        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, joke)).get();
        return true;
    }
}
