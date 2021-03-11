package events.discordevents.guildmessagereceived;

import constants.AssetIds;
import constants.Emojis;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildMessageReceivedAbstract;
import modules.ChatGameGuessingNames;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@DiscordEvent(priority = EventPriority.LOW)
public class GuildMessageReceivedAnicordChatGameGuessingNumber extends GuildMessageReceivedAbstract {

    @Override
    public boolean onGuildMessageReceived(GuildMessageReceivedEvent event) throws Throwable {
        final long GAME_CHANNEL_ID = 758285721877479504L;

        if (event.getGuild().getIdLong() == AssetIds.ANICORD_SERVER_ID && event.getChannel().getIdLong() == GAME_CHANNEL_ID) {
            String numStr = event.getMessage().getContentRaw();
            if (numStr.contains(" ")) {
                numStr = numStr.split(" ")[0];
            }

            if (StringUtil.stringIsInt(numStr)) {
                int val = Integer.parseInt(numStr);
                if (val >= 1 && val <= 100000) {
                    int tries = ChatGameGuessingNames.getInstance().getTries() + 1;
                    int res = ChatGameGuessingNames.getInstance().check(val);
                    if (res == 0) {
                        event.getMessage().addReaction(Emojis.CHECKMARK).queue();
                        event.getMessage().reply(String.format("**%s** hat richtig geraten!\nDie Lösung war: `%s` (%d Versuche)", StringUtil.escapeMarkdown(event.getMember().getEffectiveName()), StringUtil.numToString(val), tries))
                                .queue();
                    } else {
                        if (res > 0) {
                            event.getMessage().addReaction("⬆️").queue();
                        } else {
                            event.getMessage().addReaction("⬇️").queue();
                        }
                    }
                }
            }
        }

        return true;
    }

}