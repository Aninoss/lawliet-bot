package Commands.ModerationCategory;

import CommandListeners.CommandProperties;
import CommandListeners.OnReactionAddListener;
import CommandSupporters.Command;
import Constants.Permission;
import Core.EmbedFactory;
import Core.Mention.Mention;
import Core.Mention.MentionList;
import Core.Mention.MentionUtil;
import Core.TextManager;
import Core.Utils.StringUtil;
import Modules.Mod;
import MySQL.Modules.Moderation.DBModeration;
import MySQL.Modules.Moderation.ModerationBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "warn",
    userPermissions = Permission.KICK_MEMBERS | Permission.BAN_MEMBERS,
    emoji = "\uD83D\uDEA8",
    executable = false
)
public class WarnCommand extends Command implements OnReactionAddListener {

    protected final int CHAR_LIMIT = 300;

    private Message message;
    protected List<User> userList;
    private ModerationBean moderationBean;
    protected String reason;

    public WarnCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        if (!setUserListAndReason(event, followedString))
            return false;

        moderationBean = DBModeration.getInstance().getBean(event.getServer().get().getId());

        if (moderationBean.isQuestion()) {
            Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), event.getServer().get(), userList);
            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this, getString("confirmaion", reason.length() > 0, mention.toString(), reason));
            if (reason.length() > 0) eb.addField(getString("reason"), "```" + reason + "```", false);
            postMessage(event.getServerTextChannel().get(), eb);
            for(int i = 0; i < 2; i++) this.message.addReaction(StringUtil.getEmojiForBoolean(i == 0)).get();
        } else {
            return execute(event.getServerTextChannel().get(), event.getMessage().getUserAuthor().get());
        }

        return true;
    }

    private boolean setUserListAndReason(MessageCreateEvent event, String followedString) throws ExecutionException, InterruptedException {
        Message message = event.getMessage();
        MentionList<User> userMentionList = getMentionList(message, followedString);
        userList = userMentionList.getList();
        if (userList.size() == 0) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        reason = userMentionList.getResultMessageString().replace("`", "");
        reason = StringUtil.trimString(reason);
        if (reason.length() > CHAR_LIMIT) {
            message.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", String.valueOf(CHAR_LIMIT))));
            return false;
        }

        return true;
    }

    protected MentionList<User> getMentionList(Message message, String followedString) throws ExecutionException, InterruptedException {
        return MentionUtil.getUsers(message, followedString);
    }

    private boolean execute(ServerTextChannel channel, User executer) throws Throwable {
        removeReactionListener();

        ArrayList<User> usersErrorList = new ArrayList<>();
        for(User user: userList) {
            if (!canProcess(channel.getServer(), executer, user)) usersErrorList.add(user);
        }
        if (usersErrorList.size() > 0) {
            Mention mentionError = MentionUtil.getMentionedStringOfUsers(getLocale(), channel.getServer(), usersErrorList);
            postMessage(channel, EmbedFactory.getCommandEmbedError(this, getString("usererror_description", mentionError.isMultiple(), mentionError.toString()), TextManager.getString(getLocale(), TextManager.GENERAL, "missing_permissions_title")));
            return false;
        }

        Mention mention = MentionUtil.getMentionedStringOfUsers(getLocale(), channel.getServer(), userList);
        EmbedBuilder actionEmbed = EmbedFactory.getCommandEmbedStandard(this, getString("action", mention.isMultiple(), mention.toString(), executer.getMentionTag()));
        if (reason.length() > 0) actionEmbed.addField(getString("reason"), "```" + reason + "```", false);
        for(User user: userList) {
            try {
                if (!user.isYourself() && !user.isBot() && sendDM()) user.sendMessage(actionEmbed).get();
            } catch (ExecutionException e) {
                //Ignore
            }
            Mod.insertWarning(getLocale(), channel.getServer(), user, executer, reason);
            process(channel.getServer(), user);
        }

        Mod.postLog(this, actionEmbed, moderationBean);

        EmbedBuilder successEb = EmbedFactory.getCommandEmbedStandard(this, getString("success_description", mention.isMultiple(), mention.toString()));
        if (reason.length() > 0) successEb.addField(getString("reason"), "```" + reason + "```", false);
        postMessage(channel, successEb);

        return true;
    }

    protected boolean sendDM() {
        return true;
    }

    private void postMessage(ServerTextChannel channel, EmbedBuilder eb) throws ExecutionException, InterruptedException {
        if (message == null) message = channel.sendMessage(eb).get();
        else message.edit(eb).get();
    }

    public void process(Server server, User user) throws Throwable {}

    public boolean canProcess(Server server, User userStarter, User userAim) {
        return true;
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (event.getEmoji().isUnicodeEmoji()) {
            for (int i = 0; i < 2; i++) {
                if (event.getEmoji().asUnicodeEmoji().get().equals(StringUtil.getEmojiForBoolean(i == 0))) {
                    if (i == 0) {
                        execute(event.getServerTextChannel().get(), event.getUser());
                    } else {
                        removeReactionListener();
                        postMessage(event.getServerTextChannel().get(), EmbedFactory.getCommandEmbedStandard(this, getString("abort_description"), getString("abort_title")));
                    }
                }
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
