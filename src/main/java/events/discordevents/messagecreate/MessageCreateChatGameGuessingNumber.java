package events.discordevents.messagecreate;

import constants.AssetIds;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.MessageCreateAbstract;
import modules.ChatGameGuessingNames;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.util.logging.ExceptionLogger;

@DiscordEvent(priority = EventPriority.LOW)
public class MessageCreateChatGameGuessingNumber extends MessageCreateAbstract {

    @Override
    public boolean onMessageCreate(MessageCreateEvent event) throws Throwable {
        final long ANINOSS_SERVER_ID = AssetIds.ANINOSS_SERVER_ID;
        final long GAME_CHANNEL_ID = 758285721877479504L;

        if (event.getServer().map(DiscordEntity::getId).orElse(0L) == ANINOSS_SERVER_ID && event.getChannel().getId() == GAME_CHANNEL_ID) {
            String numStr = event.getMessageContent();
            if (numStr.contains(" "))
                numStr = numStr.split(" ")[0];

            if (StringUtil.stringIsInt(numStr)) {
                int val = Integer.parseInt(numStr);
                if (val >= 1 && val <= 100000) {
                    int tries = ChatGameGuessingNames.getInstance().getTries() + 1;
                    int res = ChatGameGuessingNames.getInstance().check(val);
                    if (res == 0) {
                        event.getMessage().addReaction("✅").exceptionally(ExceptionLogger.get());
                        event.getChannel().sendMessage(String.format("%s hat richtig geraten!\nDie Lösung war: `%s` (%d Versuche)", event.getMessageAuthor().asUser().get().getMentionTag(), StringUtil.numToString(val), tries));
                    } else {
                        if (res > 0)
                            event.getMessage().addReaction("⬆️").exceptionally(ExceptionLogger.get());
                        else
                            event.getMessage().addReaction("⬇️").exceptionally(ExceptionLogger.get());
                    }
                }
            }
        }

        return true;
    }

}