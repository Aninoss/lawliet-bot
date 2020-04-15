package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;

import CommandSupporters.Command;
import Constants.Permission;
import Core.CustomObservableList;
import Core.EmbedFactory;
import Core.Mention.MentionTools;
import Core.Mention.MentionList;
import Core.TextManager;
import Core.Tools.StringTools;
import MySQL.Modules.Moderation.DBModeration;
import MySQL.Modules.Warning.DBServerWarnings;
import MySQL.Modules.Warning.ServerWarningsSlot;
import javafx.util.Pair;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "warnremove",
        emoji = "\uD83D\uDDD1",
        userPermissions = Permission.MANAGE_SERVER,
        thumbnail = "http://icons.iconarchive.com/icons/martz90/circle/128/trash-icon.png",
        executable = false
)
public class WarnRemoveCommand extends Command implements OnReactionAddListener {

    final static Logger LOGGER = LoggerFactory.getLogger(WarnRemoveCommand.class);
    private ArrayList<User> users;
    private int n;
    private String nString, userString;
    private User requestor;
    private ServerTextChannel channel;
    private Message message;

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        channel = event.getServerTextChannel().get();
        requestor = event.getMessage().getUserAuthor().get();
        MentionList<User> userMentions = MentionTools.getUsers(event.getMessage(), followedString);
        users = userMentions.getList();
        followedString = StringTools.trimString(userMentions.getResultMessageString());

        if (users.size() == 0) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        boolean removeAll = followedString.equalsIgnoreCase("all");

        if (!removeAll && !StringTools.stringIsInt(followedString)) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "no_digit"))).get();
            return false;
        }

        n = removeAll ? 99999 : Integer.parseInt(followedString);
        if (n < 1 || n > 99999) {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL, "number", "1", "99999"))).get();
            return false;
        }

        nString = removeAll ? getString("all") : StringTools.numToString(getLocale(), n);
        userString = MentionTools.getMentionedStringOfUsers(getLocale(), event.getServer().get(), users).getString();

        if (DBModeration.getInstance().getBean(channel.getServer().getId()).isQuestion()) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("confirmation", n != 1, nString, userString));
            postMessage(eb);
            for(int i = 0; i < 2; i++) this.message.addReaction(StringTools.getEmojiForBoolean(i == 0)).get();
        } else {
            executeRemoval();
        }

        return true;
    }

    private void executeRemoval() throws IOException, ExecutionException, InterruptedException {
        removeReactionListener();

        for(User user: users) {
            CustomObservableList<ServerWarningsSlot> serverWarningsSlots = DBServerWarnings.getInstance().getBean(new Pair<>(channel.getServer().getId(), user.getId())).getWarnings();
            serverWarningsSlots.remove(Math.max(0, serverWarningsSlots.size() - n), serverWarningsSlots.size());
        }

        postMessage(EmbedFactory.getCommandEmbedSuccess(this,
                getString("success", n != 1, nString, userString)
        ));

        DBModeration.getInstance().getBean(channel.getServer().getId()).getAnnouncementChannel().ifPresent(serverTextChannel -> {
            try {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this,
                        getString("modlog", n != 1, requestor.getMentionTag(), nString, userString)
                );
                serverTextChannel.sendMessage(eb).get();
            } catch (IOException | InterruptedException | ExecutionException e) {
                LOGGER.error("Could not send mod log", e);
            }
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
                if (event.getEmoji().asUnicodeEmoji().get().equals(StringTools.getEmojiForBoolean(i == 0))) {
                    if (i == 0) {
                        executeRemoval();
                    } else {
                        removeReactionListener();
                        postMessage(EmbedFactory.getCommandEmbedStandard(
                                        this,
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, "warn_abort_description"),
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, "warn_abort_title")
                                )
                        );
                    }
                }
            }
        }
    }

}
