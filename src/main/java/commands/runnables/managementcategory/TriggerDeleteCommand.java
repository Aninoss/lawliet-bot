package commands.runnables.managementcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.Command;
import constants.Permission;
import core.EmbedFactory;
import core.utils.StringUtil;
import mysql.modules.server.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "triggerdelete",
        userPermissions = Permission.MANAGE_SERVER | Permission.MANAGE_MESSAGES,
        emoji = "\uD83D\uDDD1",
        executable = true,
        patreonRequired = true,
        aliases = { "triggerremove", "starterremove", "startermessagedelete", "startermessageremove", "messagedelete", "messageremove", "starterdelete" }
)
public class TriggerDeleteCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] activeArgs = new String[]{"off", "on"};

    public TriggerDeleteCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (followedString.length() > 0) {
            int option = -1;
            for(int i=0; i < activeArgs.length; i++) {
                String str = activeArgs[i];
                if (followedString.equalsIgnoreCase(str)) option = i;
            }

            if (option == -1) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("invalid", followedString)));
                return false;
            }

            boolean active = option == 1;
            DBServer.getInstance().getBean(event.getServer().get().getId()).setCommandAuthorMessageRemove(active);
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("set", active))).get();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), DBServer.getInstance().getBean(event.getServer().get().getId()).isCommandAuthorMessageRemove());
            message = event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("reaction", onOffText))).get();
            for(int i = 0; i < 2; i++) {
                message.addReaction(StringUtil.getEmojiForBoolean(i == 1));
            }
            return true;
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        for(int i = 0; i < 2; i++) {
            String str = StringUtil.getEmojiForBoolean(i == 1);
            if (event.getEmoji().getMentionTag().equalsIgnoreCase(str)) {
                boolean active = i == 1;
                DBServer.getInstance().getBean(event.getServer().get().getId()).setCommandAuthorMessageRemove(active);
                getReactionMessage().edit(EmbedFactory.getCommandEmbedStandard(this, getString("set", active))).get();
                removeReactionListener(getReactionMessage());
                return;
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) {}

}
