package Commands.General;

import CommandListeners.*;
import CommandSupporters.Command;
import General.*;
import General.BotResources.ResourceManager;
import General.Mention.Mention;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

public class RewardCommand extends Command implements onRecievedListener  {
    public RewardCommand() {
        super();
        trigger = "reward";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "https://i.pinimg.com/236x/5b/82/b7/5b82b79d524af5731b49cbee4dc829ce--death-note-.jpg";
        emoji = "\uD83C\uDF53";
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

        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,getString("template_single",mention.getString()))
                .setImage(ResourceManager.getFile(ResourceManager.RESOURCES,"reward.gif"));

        if (mention.isMultiple())
            eb.setDescription(getString("template_multiple",mention.getString()));

        message.getChannel().sendMessage(eb).get();
        removeMessageForwarder();

        return true;
    }
}
