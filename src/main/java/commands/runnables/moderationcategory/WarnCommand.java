package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import commands.listeners.OnReactionAddListener;
import commands.Command;
import core.EmbedFactory;
import core.mention.Mention;
import core.mention.MentionList;
import core.utils.MentionUtil;
import core.TextManager;
import core.utils.StringUtil;
import modules.Mod;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationBean;








import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
    trigger = "warn",
    userPermissions = PermissionDeprecated.KICK_MEMBERS,
    emoji = "\uD83D\uDEA8",
    executableWithoutArgs = false
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

        moderationBean = DBModeration.getInstance().retrieve(event.getServer().get().getId());

        if (userList.size() > 1 || moderationBean.isQuestion()) {
            Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), userList);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("confirmaion", reason.length() > 0, mention.getMentionText(), reason));
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
            message.getChannel().sendMessage(EmbedFactory.getEmbedError(this,
                    TextManager.getString(getLocale(), TextManager.GENERAL,"no_mentions"))).get();
            return false;
        }

        reason = userMentionList.getResultMessageString().replace("`", "");
        reason = reason.trim();
        if (reason.length() > CHAR_LIMIT) {
            message.getChannel().sendMessage(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", String.valueOf(CHAR_LIMIT))));
            return false;
        }

        return true;
    }

    protected MentionList<User> getMentionList(Message message, String followedString) throws ExecutionException, InterruptedException {
        return MentionUtil.getMembers(message, followedString);
    }

    private boolean execute(ServerTextChannel channel, User executer) throws Throwable {
        removeReactionListener();

        ArrayList<User> usersErrorList = new ArrayList<>();
        for(User user: userList) {
            if (!canProcess(channel.getServer(), executer, user)) usersErrorList.add(user);
        }
        if (usersErrorList.size() > 0) {
            Mention mentionError = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), usersErrorList);
            postMessage(channel, EmbedFactory.getEmbedError(this, getString("usererror_description", mentionError.isMultiple(), mentionError.getMentionText()), TextManager.getString(getLocale(), TextManager.GENERAL, "missing_permissions_title")));
            return false;
        }

        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), userList);
        EmbedBuilder actionEmbed = EmbedFactory.getEmbedDefault(this, getString("action", mention.isMultiple(), mention.getMentionText(), executer.getMentionTag(), StringUtil.escapeMarkdown(channel.getServer().getName())));
        if (reason.length() > 0) actionEmbed.addField(getString("reason"), "```" + reason + "```", false);

        Mod.postLog(this, actionEmbed, moderationBean, userList).join();
        for(User user: userList) {
            if (sendWarning())
                Mod.insertWarning(getLocale(), channel.getServer(), user, executer, reason, autoActions());
            process(channel.getServer(), user);
        }

        EmbedBuilder successEb = EmbedFactory.getEmbedDefault(this, getString("success_description", mention.isMultiple(), mention.getMentionText()));
        if (reason.length() > 0) successEb.addField(getString("reason"), "```" + reason + "```", false);
        postMessage(channel, successEb);

        return true;
    }

    protected boolean sendDM() {
        return true;
    }

    protected boolean sendWarning() {
        return true;
    }

    protected boolean autoActions() {
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
                        execute(event.getServerTextChannel().get(),  event.getUser().get());
                    } else {
                        removeReactionListener();
                        postMessage(event.getServerTextChannel().get(), EmbedFactory.getAbortEmbed(this));
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
