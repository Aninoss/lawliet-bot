package commands.runnables.moderationcategory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.Command;
import commands.listeners.CommandProperties;
import core.CustomObservableList;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import mysql.modules.moderation.DBModeration;
import mysql.modules.warning.DBServerWarnings;
import mysql.modules.warning.GuildWarningsSlot;
import net.dv8tion.jda.api.EmbedBuilder;

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
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        channel = event.getServerTextChannel().get();
        requestor = event.getMessage().getUserAuthor().get();
        MentionList<User> userMentions = MentionUtil.getMembers(event.getMessage(), args);
        users = userMentions.getList();
        args = userMentions.getResultMessageString().trim();

        if (users.size() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        boolean removeAll = args.equalsIgnoreCase("all");

        if (!removeAll && !StringUtil.stringIsInt(args)) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
            return false;
        }

        n = removeAll ? 99999 : Integer.parseInt(args);
        if (n < 1 || n > 99999) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "99999"))).get();
            return false;
        }

        nString = removeAll ? getString("all") : StringUtil.numToString(n);
        userString = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), users).getMentionText();

        if (DBModeration.getInstance().retrieve(channel.getServer().getId()).isQuestion()) {
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
            CustomObservableList<GuildWarningsSlot> serverWarningsSlots = DBServerWarnings.getInstance().retrieve(new Pair<>(channel.getServer().getId(), user.getId())).getWarnings();
            serverWarningsSlots.remove(Math.max(0, serverWarningsSlots.size() - n), serverWarningsSlots.size());
        }

        postMessage(EmbedFactory.getEmbedDefault(this,
                getString("success", n != 1, nString, userString)
        ));

        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this,
                getString("modlog", n != 1, requestor.getMentionTag(), nString, userString)
        );

        users.forEach(user -> user.sendMessage(eb).exceptionally(ExceptionLogger.get()));
        DBModeration.getInstance().retrieve(channel.getServer().getId()).getAnnouncementChannel().ifPresent(serverTextChannel -> {
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
