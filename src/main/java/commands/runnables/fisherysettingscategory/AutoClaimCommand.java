package commands.runnables.fisherysettingscategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import core.EmbedFactory;
import core.utils.DiscordUtil;
import core.utils.StringUtil;
import mysql.modules.autoclaim.AutoClaimBean;
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
        executableWithoutArgs = true
)
public class AutoClaimCommand extends Command implements OnReactionAddListener {

    private Message message;

    private final String[] activeArgs = new String[]{"off", "on"};

    public AutoClaimCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        User user = event.getMessageAuthor().asUser().get();
        AutoClaimBean autoClaimBean = DBAutoClaim.getInstance().retrieve();

        if (followedString.length() > 0) {
            int option = -1;
            for(int i = 0; i < activeArgs.length; i++) {
                String str = activeArgs[i];
                if (followedString.equalsIgnoreCase(str)) option = i;
            }

            if (option == -1) {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(this, getString("invalid", followedString)));
                return false;
            }

            boolean active = option == 1;
            autoClaimBean.setActive(user.getId(), active);
            event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("set", active, user.getMentionTag()))).get();
            return true;
        } else {
            String onOffText = StringUtil.getOnOffForBoolean(getLocale(), autoClaimBean.isActive(user.getId()));
            message = event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("reaction", onOffText))).get();
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
            if (DiscordUtil.emojiIsString(event.getEmoji(), str)) {
                boolean active = i == 1;
                DBAutoClaim.getInstance().retrieve().setActive(event.getUserId(), active);
                getReactionMessage().edit(EmbedFactory.getEmbedDefault(this, getString("set", active, event.getUser().get().getMentionTag()))).get();
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
