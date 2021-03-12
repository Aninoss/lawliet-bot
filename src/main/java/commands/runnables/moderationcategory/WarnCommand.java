package commands.runnables.moderationcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.listeners.OnReactionListener;
import constants.Emojis;
import core.EmbedFactory;
import core.TextManager;
import core.mention.Mention;
import core.mention.MentionList;
import core.utils.JDAEmojiUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Mod;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationBean;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "warn",
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83D\uDEA8",
        executableWithoutArgs = false
)
public class WarnCommand extends Command implements OnReactionListener {

    private enum Status { PENDING, CANCELED, COMPLETED, ERROR }

    protected final int CHAR_LIMIT = 300;

    private List<User> userList;
    private String reason;
    private ModerationBean moderationBean;
    private Status status = Status.PENDING;
    private final ArrayList<User> usersErrorList = new ArrayList<>();
    private final boolean sendWarning;
    private final boolean autoActions;

    public WarnCommand(Locale locale, String prefix) {
        this(locale, prefix, true, true);
    }

    public WarnCommand(Locale locale, String prefix, boolean sendWarning, boolean autoActions) {
        super(locale, prefix);
        this.sendWarning = sendWarning;
        this.autoActions = autoActions;
    }

    protected MentionList<User> getUserList(Message message, String args) throws Throwable {
        MentionList<Member> memberMentionList = MentionUtil.getMembers(message, args);
        List<User> userList = memberMentionList.getList().stream()
                .map(Member::getUser)
                .collect(Collectors.toList());

        return new MentionList<>(memberMentionList.getFilteredArgs(), userList);
    }

    protected void process(Guild guild, User target, String reason) throws Throwable {
    }

    protected boolean canProcess(Member executor, User target) throws Throwable {
        return true;
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        if (!setUserListAndReason(event, args)) {
            return false;
        }

        moderationBean = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        if (userList.size() > 1 || moderationBean.isQuestion()) {
            registerReactionListener(Emojis.CHECKMARK, Emojis.X);
        } else {
            boolean success = execute(event.getChannel(), event.getMember());
            drawMessage(draw());
            return success;
        }

        return true;
    }

    protected boolean setUserListAndReason(GuildMessageReceivedEvent event, String args) throws Throwable {
        Message message = event.getMessage();
        MentionList<User> userMention = getUserList(message, args);
        userList = userMention.getList();
        if (userList.size() == 0) {
            message.getChannel().sendMessage(
                    EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions")).build()
            ).queue();
            return false;
        }

        reason = userMention.getFilteredArgs().trim();
        reason = StringUtil.shortenString(reason, CHAR_LIMIT);

        return true;
    }

    private boolean execute(TextChannel channel, Member executor) throws Throwable {
        for (User user : userList) {
            if (!canProcess(executor, user)) {
                usersErrorList.add(user);
            }
        }
        if (usersErrorList.size() > 0) {
            status = Status.ERROR;
            return false;
        }

        EmbedBuilder actionEmbed = getActionEmbed(executor, channel);
        if (reason.length() > 0) {
            actionEmbed.addField(getString("reason"), "```" + reason + "```", false);
        }

        Mod.postLogUsers(this, actionEmbed, moderationBean, userList).join();
        for (User user : userList) {
            if (sendWarning) {
                Mod.insertWarning(getLocale(), channel.getGuild(), user, executor, reason, autoActions);
            }
            process(channel.getGuild(), user, reason);
        }

        status = Status.COMPLETED;
        return true;
    }

    protected EmbedBuilder getActionEmbed(Member executor, TextChannel channel) {
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), userList);
        return EmbedFactory.getEmbedDefault(this, getString("action", mention.isMultiple(), mention.getMentionText(), executor.getAsMention(), StringUtil.escapeMarkdown(channel.getGuild().getName())));
    }

    protected EmbedBuilder getConfirmationEmbed() {
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), userList);
        return EmbedFactory.getEmbedDefault(this, getString("confirmaion", mention.getMentionText()));
    }

    protected EmbedBuilder getSuccessEmbed() {
        Mention mention = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), userList);
        return EmbedFactory.getEmbedDefault(this, getString("success_description", mention.isMultiple(), mention.getMentionText()));
    }

    @Override
    public boolean onReaction(GenericGuildMessageReactionEvent event) throws Throwable {
        for (int i = 0; i < 2; i++) {
            if (JDAEmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), StringUtil.getEmojiForBoolean(i == 0))) {
                removeReactionListener();
                if (i == 0) {
                    execute(event.getChannel(), event.getMember());
                } else {
                    status = Status.CANCELED;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public EmbedBuilder draw() {
        switch (status) {
            case COMPLETED:
                EmbedBuilder eb = getSuccessEmbed();
                if (reason.length() > 0) {
                    eb.addField(getString("reason"), "```" + StringUtil.escapeMarkdownInField(reason) + "```", false);
                }
                return eb;

            case CANCELED:
                return EmbedFactory.getAbortEmbed(this);

            case ERROR:
                Mention mentionError = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), usersErrorList);
                return EmbedFactory.getEmbedError(this, getString("usererror_description", mentionError.isMultiple(), mentionError.getMentionText()), TextManager.getString(getLocale(), TextManager.GENERAL, "missing_permissions_title"));

            default:
                eb = getConfirmationEmbed();
                if (reason.length() > 0) {
                    eb.addField(getString("reason"), "```" + reason + "```", false);
                }
                return eb;
        }
    }

    public List<User> getUserList() {
        return userList;
    }

    public String getReason() {
        return reason;
    }

}
