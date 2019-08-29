package Commands.General;

import CommandListeners.*;
import CommandSupporters.Command;
import General.EmbedFactory;
import General.Tools;
import General.BotResources.ResourceManager;
import General.TextManager;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.ArrayList;

public class FortuneCommand extends Command implements onRecievedListener {
    private static ArrayList<Integer> picked = new ArrayList<>();

    public FortuneCommand() {
        super();
        trigger = "fortune";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/android-settings/128/question-icon.png";
        emoji = "❓";
        executable = false;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
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

    private EmbedBuilder getEmbed(Message message, String question) throws Throwable {
        int n = Tools.pickFullRandom(picked,TextManager.getKeySize(locale,TextManager.ANSWERS)-1);
        String answerRaw = TextManager.getString(locale,TextManager.ANSWERS, String.valueOf(n+1));
        String answer = answerRaw;
        if (answer.equals("%RandomUpperCase")) {
            answer = Tools.randomUpperCase(question);
        } else if (answer.startsWith("%Gif")) answer = "";
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("template",message.getAuthor().getDisplayName(),question,answer));

        if (answerRaw.equals("%GifNo")) eb.setImage(ResourceManager.getFile(ResourceManager.RESOURCES,"godno.jpg"));
        if (answerRaw.equals("%GifYes")) eb.setImage(ResourceManager.getFile(ResourceManager.RESOURCES,"yes.gif"));

        return eb;
    }
}
