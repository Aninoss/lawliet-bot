package Commands;

import CommandListeners.CommandProperties;
import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import Constants.PowerPlantStatus;
import General.*;
import MySQL.DBServer;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public abstract class ListAbstract extends Command implements onReactionAddListener {

    private Message message;
    private int page, entriesPerPage, size;
    private final String[] SCROLL_EMOJIS = {"⏪", "⏩"};

    protected abstract Pair<String, String> getEntry(ServerTextChannel channel, int i) throws Throwable;
    protected abstract int getSize();
    protected abstract int getEntriesPerPage();

    protected void init(ServerTextChannel channel) throws Throwable {
        entriesPerPage = getEntriesPerPage();
        size = getSize();
        page = 0;
        message = channel.sendMessage(getEmbed(channel)).get();
        if (getPageSize() <= 1) message = null;
        else for(String reactionString: SCROLL_EMOJIS) message.addReaction(reactionString).get();
    }

    private EmbedBuilder getEmbed(ServerTextChannel channel) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this)
                .setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "list_footer", String.valueOf(page + 1), String.valueOf(getPageSize())));

        for(int i = page * entriesPerPage; i < Math.min(size, page * entriesPerPage + entriesPerPage); i++) {
            Pair<String, String> entry = getEntry(channel, i);
            eb.addField(entry.getKey(), entry.getValue(), false);
        }

        return eb;
    }

    private int getPageSize() {
        return ((size - 1) / entriesPerPage) + 1;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!event.getEmoji().isUnicodeEmoji()) return;
        if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(SCROLL_EMOJIS[0])) {
            page--;
            if (page < 0) page = getPageSize() - 1;
            message.edit(getEmbed(event.getServerTextChannel().get()));
        } else if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(SCROLL_EMOJIS[1])) {
            page++;
            if (page > getPageSize() - 1) page = 0;
            message.edit(getEmbed(event.getServerTextChannel().get()));
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}

}
