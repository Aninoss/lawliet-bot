package commands.runnables.moderationcategory;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import constants.PermissionDeprecated;
import core.CustomObservableList;
import core.EmbedFactory;
import core.TextManager;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.moderation.DBModeration;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.ServerWarningsSlot;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.javacord.api.util.logging.ExceptionLogger;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "warnremove",
        emoji = "\uD83D\uDDD1",
        userPermissions = PermissionDeprecated.KICK_MEMBERS,
        executableWithoutArgs = false,
        aliases = { "unwarn", "removewarn" }
)
public class WarnRemoveCommand extends Command implements OnReactionAddListener {

    private ArrayList<User> users;
    private int n;
    private String nString, userString;
    private User requestor;
    private ServerTextChannel channel;
    private Message message;

    public WarnRemoveCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        channel = event.getServerTextChannel().get();
        requestor = event.getMessage().getUserAuthor().get();
        MentionList<User> userMentions = MentionUtil.getMembers(event.getMessage(), followedString);
        users = userMentions.getList();
        followedString = StringUtil.trimString(userMentions.getResultMessageString());

        if (users.size() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        boolean removeAll = followedString.equalsIgnoreCase("all");

        if (!removeAll && !StringUtil.stringIsInt(followedString)) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
            return false;
        }

        n = removeAll ? 99999 : Integer.parseInt(followedString);
        if (n < 1 || n > 99999) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "99999"))).get();
            return false;
        }

        nString = removeAll ? getString("all") : StringUtil.numToString(n);
        userString = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), users).getMentionText();

        if (DBModeration.getInstance().getBean(channel.getServer().getId()).isQuestion()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("confirmation", n != 1, nString, userString));
            postMessage(eb);
            for(int i = 0; i < 2; i++) this.message.addReaction(StringUtil.getEmojiForBoolean(i == 0)).get();
        } else {
            executeRemoval();
        }

        return true;
    }

    private void executeRemoval() throws ExecutionException, InterruptedException {
        removeReactionListener();

        for(User user: users) {
            CustomObservableList<ServerWarningsSlot> serverWarningsSlots = DBServerWarnings.getInstance().getBean(new Pair<>(channel.getServer().getId(), user.getId())).getWarnings();
            serverWarningsSlots.remove(Math.max(0, serverWarningsSlots.size() - n), serverWarningsSlots.size());
        }

        postMessage(EmbedFactory.getEmbedDefault(this,
                getString("success", n != 1, nString, userString)
        ));

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this,
                getString("modlog", n != 1, requestor.getMentionTag(), nString, userString)
        );

        users.forEach(user -> user.sendMessage(eb).exceptionally(ExceptionLogger.get()));
        DBModeration.getInstance().getBean(channel.getServer().getId()).getAnnouncementChannel().ifPresent(serverTextChannel -> {
            serverTextChannel.sendMessage(eb).exceptionally(ExceptionLogger.get());
        });
    }

    private void postMessage(EmbedBuilder eb) throws ExecutionException, InterruptedException {
        if (message == null) message = channel.sendMessage(eb).get();
        else message.edit(eb).get();
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji()) {
            for (int i = 0; i < 2; i++) {
                if (event.getEmoji().asUnicodeEmoji().get().equals(StringUtil.getEmojiForBoolean(i == 0))) {
                    if (i == 0) {
                        executeRemoval();
                    } else {
                        removeReactionListener();
                        postMessage(EmbedFactory.getAbortEmbed(this));
                    }
                }
            }
        }
    }

}
