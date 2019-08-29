package Commands.General;

import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.*;
import General.BotResources.ResourceManager;
import General.Mention.Mention;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public class EveryoneCommand extends Command implements onRecievedListener  {

    public EveryoneCommand() {
        super();
        trigger = "everyone";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        emoji = "\uD83D\uDE21";
        executable = false;
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        Message message = event.getMessage();
        Mention mention = Tools.getMentionedString(locale,message,followedString);
        if (mention == null) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(locale,TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                getString("template_single",mention.getString()))
                .setImage(ResourceManager.getFolder(ResourceManager.SPAM).getRandomFile().getURL());

        if (mention.isMultiple())
            eb.setDescription(getString("template_multiple",mention.getString()));

        message.getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }
}
