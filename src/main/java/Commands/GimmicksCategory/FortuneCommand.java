package Commands.GimmicksCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import Core.EmbedFactory;
import Core.Tools.RandomTools;
import Core.BotResources.ResourceManager;
import Core.TextManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "fortune",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/android-settings/128/question-icon.png",
        emoji = "❓",
        executable = false,
        aliases = {"question"}
)
public class FortuneCommand extends Command {

    private static ArrayList<Integer> picked = new ArrayList<>();

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        if (followedString.length() > 0) {
            event.getChannel().sendMessage(getEmbed(message,followedString)).get();
            return true;
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    getString("no_arg"))).get();
            return false;
        }
    }

    private EmbedBuilder getEmbed(Message message, String question) throws IOException, ExecutionException, InterruptedException {
        int n = RandomTools.pickFullRandom(picked,TextManager.getKeySize(getLocale(),TextManager.ANSWERS));
        String answerRaw = TextManager.getString(getLocale(),TextManager.ANSWERS, String.valueOf(n+1));
        String answer = answerRaw;
        if (answer.equals("%RandomUpperCase")) {
            answer = RandomTools.randomUpperCase(question);
        } else if (answer.startsWith("%Gif")) answer = "";
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("template",message.getAuthor().getDisplayName(),question,answer));

        if (answerRaw.equals("%GifNo")) eb.setImage(ResourceManager.getFile(ResourceManager.RESOURCES,"godno.jpg"));
        if (answerRaw.equals("%GifYes")) eb.setImage(ResourceManager.getFile(ResourceManager.RESOURCES,"yes.gif"));

        return eb;
    }
}
