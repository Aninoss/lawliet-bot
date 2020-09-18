package commands.commandslots.fisherysettingscategory;

import commands.commandlisteners.CommandProperties;
import commands.commandlisteners.OnReactionAddListener;
import commands.Command;
import core.EmbedFactory;
import core.utils.StringUtil;
import mysql.modules.autoclaim.DBAutoClaim;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.Locale;

@CommandProperties(
        trigger = "autoclaim",
        emoji = "\uD83E\uDD16",
        patreonRequired = true,
        executable = true
)
public class AutoClaimCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] activeArgs = new String[]{"off", "on"};

    public AutoClaimCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        User user = event.getMessageAuthor().asUser().get();

        if (followedString.length() > 0) {
            int option = -1;
            for(int i = 0; i < activeArgs.length; i++) {
                String str = activeArgs[i];
                if (followedString.equalsIgnoreCase(str)) option = i;
            }

            if (option == -1) {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, getString("invalid", followedString)));
                return false;
            }

            boolean active = option == 1;
            DBAutoClaim.getInstance().getBean(user.getId()).setActive(active);
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, getString("set", active, user.getMentionTag()))).get();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), DBAutoClaim.getInstance().getBean(user.getId()).isActive());
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
                DBAutoClaim.getInstance().getBean(event.getUser().getId()).setActive(active);
                getReactionMessage().edit(EmbedFactory.getCommandEmbedStandard(this, getString("set", active, event.getUser().getMentionTag()))).get();
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
