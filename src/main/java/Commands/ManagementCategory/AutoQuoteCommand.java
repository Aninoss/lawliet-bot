package Commands.ManagementCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;

import CommandSupporters.Command;
import Constants.Permission;
import Core.EmbedFactory;
import Core.Utils.StringUtil;
import MySQL.Modules.AutoQuote.DBAutoQuote;
import org.javacord.api.entity.message.Message;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

@CommandProperties(
        trigger = "autoquote",
        userPermissions = Permission.MANAGE_SERVER,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Book-icon.png",
        emoji = "\uD83D\uDCDD",
        executable = true
)
public class AutoQuoteCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] activeArgs = new String[]{"off", "on"};

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
            DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).setActive(active);
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedSuccess(this, getString("set", active))).get();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).isActive());
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
                DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).setActive(active);
                getReactionMessage().edit(EmbedFactory.getCommandEmbedSuccess(this, getString("set", active))).get();
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
