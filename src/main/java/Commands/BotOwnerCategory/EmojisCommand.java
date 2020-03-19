package Commands.BotOwnerCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import General.EmbedFactory;
import org.javacord.api.entity.emoji.CustomEmoji;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

@CommandProperties(
    trigger = "emojis",
    privateUse = true,
    emoji = "\uD83D\uDE03",
    executable = true
)
public class EmojisCommand extends Command implements onRecievedListener {

    public EmojisCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        StringBuilder sb = new StringBuilder();
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        int i = 0, j = 0;
        for(CustomEmoji customEmoji: event.getServer().get().getCustomEmojis()) {
            sb.append(customEmoji.getMentionTag()).append(" | ").append(customEmoji.getId()).append("\n");
            i++;
            if (i >= 20) {
                eb.addField(String.valueOf(j*10+1)+"-"+String.valueOf(j*10+10),sb.toString());
                sb = new StringBuilder();
                i = 0;
                j++;
            }
        }
        if (i > 0) {
            eb.addField(String.valueOf(j*10+1)+"-"+String.valueOf(j*10+10),sb.toString());
        }
        event.getChannel().sendMessage(eb).get();
        return true;
    }

}
