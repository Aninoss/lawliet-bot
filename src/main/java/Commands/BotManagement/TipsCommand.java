package Commands.BotManagement;

import CommandListeners.CommandProperties;
import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.PowerPlantStatus;
import General.*;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.Random;

@CommandProperties(
    trigger = "tips",
    thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/bulb-icon.png",
    emoji = "\uD83D\uDCA1️",
    executable = true
)
public class TipsCommand extends Command implements onRecievedListener, onReactionAddListener {
    private Message message;
    private int page;
    private final int TIPS_NUMBER = 15;
    private final String[] SCROLL_EMOJIS = {"⏪", "⏩"};
    private final int TIPS_PER_PAGE = 1;

    public TipsCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        page = 0;

        if (followedString.length() > 0) {
            if (Tools.stringIsNumeric(followedString)) {
                int value = Integer.parseInt(followedString);
                if (value > 0) {
                    if (value <= getPageSize()) {
                        page = value - 1;
                    } else {
                        event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_large", String.valueOf(getPageSize())))).get();
                        return false;
                    }
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "too_small", "1"))).get();
                    return false;
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
                return false;
            }
        }

        message = event.getChannel().sendMessage(getEmbed(event.getServer().get())).get();
        for(String reactionString: SCROLL_EMOJIS) message.addReaction(reactionString).get();
        return true;
    }

    private EmbedBuilder getEmbed(Server server) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        for(int i = page*TIPS_PER_PAGE; i < Math.min(TIPS_NUMBER, page*TIPS_PER_PAGE + TIPS_PER_PAGE); i++) {
            eb.addField(getString(i+"_title"), getString(i+"_description"), true);
            eb.setFooter(getString("footer", String.valueOf(page+1), String.valueOf(getPageSize())));
        }

        return eb;
    }

    private int getPageSize() {
        return (int) Math.ceil((double) TIPS_NUMBER / TIPS_PER_PAGE);
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!event.getEmoji().isUnicodeEmoji()) return;
        if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(SCROLL_EMOJIS[0]) && page > 0) {
            page--;
            message.edit(getEmbed(event.getServer().get()));
        } else if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(SCROLL_EMOJIS[1]) && page < getPageSize() - 1) {
            page++;
            message.edit(getEmbed(event.getServer().get()));
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
