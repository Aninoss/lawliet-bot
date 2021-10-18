package commands.runnables.moderationcategory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.Category;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.mention.Mention;
import core.mention.MentionList;
import core.utils.BotPermissionUtil;
import core.utils.JDAUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Mod;
import mysql.modules.moderation.DBModeration;
import mysql.modules.moderation.ModerationData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;

@CommandProperties(
        trigger = "warn",
        userGuildPermissions = Permission.KICK_MEMBERS,
        emoji = "\uD83D\uDEA8",
        executableWithoutArgs = false,
        requiresFullMemberCache = true
)
public class WarnCommand extends Command implements OnButtonListener {

    private enum Status { PENDING, CANCELED, COMPLETED, ERROR }

    protected final int CHAR_LIMIT = 300;

    private List<User> userList;
    private String reason;
    private ModerationData moderationBean;
    private Status status = Status.PENDING;
    private final ArrayList<User> memberMissingAccessList = new ArrayList<>();
    private final ArrayList<User> botMissingAcccessList = new ArrayList<>();
    private final boolean sendWarning;
    private final boolean autoActions;
    private final boolean includeNotInGuild;
    private final boolean sendLogWarnings;

    public WarnCommand(Locale locale, String prefix) {
        this(locale, prefix, true, true, true, true);
    }

    public WarnCommand(Locale locale, String prefix, boolean sendWarning, boolean autoActions, boolean includeNotInGuild, boolean sendLogWarnings) {
        super(locale, prefix);
        this.sendWarning = sendWarning;
        this.autoActions = autoActions;
        this.includeNotInGuild = includeNotInGuild;
        this.sendLogWarnings = sendLogWarnings;
    }

    protected MentionList<User> getUserList(CommandEvent event, String args) throws Throwable {
        Guild guild = event.getGuild();
        MentionList<Member> memberMention = MentionUtil.getMembers(guild, args, event.getRepliedMember());
        ArrayList<User> userList = memberMention.getList().stream()
                .map(Member::getUser)
                .collect(Collectors.toCollection(ArrayList::new));

        if (includeNotInGuild) {
            MentionList<User> userMention = MentionUtil.getUsersFromString(memberMention.getFilteredArgs(), false).get();
            userMention.getList().forEach(user -> {
                if (!userList.contains(user)) {
                    userList.add(user);
                }
            });

            return new MentionList<>(userMention.getFilteredArgs(), userList);
        } else {
            return new MentionList<>(memberMention.getFilteredArgs(), userList);
        }
    }

    protected void process(Guild guild, User target, String reason) throws Throwable {
    }

    protected boolean canProcessMember(Member executor, User target) throws Throwable {
        return BotPermissionUtil.canInteract(executor, target);
    }

    protected boolean canProcessBot(Guild guild, User target) throws Throwable {
        return BotPermissionUtil.canInteract(guild, target);
    }

    @Override
    public boolean onTrigger(CommandEvent event, String args) throws Throwable {
        if (!setUserListAndReason(event, args)) {
            return false;
        }

        checkAllProcess(event.getMember());
        if (memberMissingAccessList.size() > 0 || botMissingAcccessList.size() > 0) {
            status = Status.ERROR;
            drawMessage(draw(event.getMember())).exceptionally(ExceptionLogger.get());
            return false;
        }

        moderationBean = DBModeration.getInstance().retrieve(event.getGuild().getIdLong());
        if (userList.size() > 1 || moderationBean.getQuestion()) {
            setComponents(
                    Button.of(ButtonStyle.SUCCESS, "true", TextManager.getString(getLocale(), Category.MODERATION, "warn_button_confirm")),
                    Button.of(ButtonStyle.SECONDARY, "false", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
            registerButtonListener(event.getMember());
        } else {
            boolean success = execute(event.getChannel(), event.getMember());
            drawMessage(draw(event.getMember())).exceptionally(ExceptionLogger.get());
            return success;
        }

        return true;
    }

    private void checkAllProcess(Member executor) throws Throwable {
        for (User user : userList) {
            if (!canProcessBot(executor.getGuild(), user)) {
                botMissingAcccessList.add(user);
            } else if (!canProcessMember(executor, user)) {
                memberMissingAccessList.add(user);
            }
        }
    }

    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        MentionList<User> userMention = getUserList(event, args);
        userList = userMention.getList();
        if (userList.size() == 0) {
            drawMessageNew(getNoMentionEmbed())
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        reason = userMention.getFilteredArgs().trim();
        reason = JDAUtil.resolveMentions(event.getGuild(), reason);
        reason = StringUtil.shortenString(reason, CHAR_LIMIT);

        return true;
    }

    private boolean execute(TextChannel channel, Member executor) throws Throwable {
        checkAllProcess(executor);
        if (memberMissingAccessList.size() > 0 || botMissingAcccessList.size() > 0) {
            status = Status.ERROR;
            return false;
        }

        EmbedBuilder actionEmbed = getActionEmbed(executor, channel);
        if (reason.length() > 0) {
            actionEmbed.addField(TextManager.getString(getLocale(), Category.MODERATION, "warn_reason"), "```" + reason + "```", false);
        }

        if (sendLogWarnings) {
            Mod.postLogUsers(this, actionEmbed, channel.getGuild(), moderationBean, userList).join();
        } else {
            Mod.sendAnnouncement(this, actionEmbed, DBModeration.getInstance().retrieve(channel.getGuild().getIdLong()));
        }

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

    protected EmbedBuilder getNoMentionEmbed() {
        return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions"));
    }

    @Override
    public boolean onButton(ButtonClickEvent event) throws Throwable {
        deregisterListenersWithComponents();
        boolean confirm = Boolean.parseBoolean(event.getComponentId());
        if (confirm) {
            event.deferEdit().queue();
            execute(event.getTextChannel(), event.getMember());
        } else {
            status = Status.CANCELED;
        }
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) {
        switch (status) {
            case COMPLETED:
                EmbedBuilder eb = getSuccessEmbed();
                if (reason.length() > 0) {
                    eb.addField(TextManager.getString(getLocale(), Category.MODERATION, "warn_reason"), "```" + StringUtil.escapeMarkdownInField(reason) + "```", false);
                }
                return eb;

            case CANCELED:
                return EmbedFactory.getAbortEmbed(this);

            case ERROR:
                Mention mentionError;
                int i;
                if (botMissingAcccessList.size() > 0) {
                    i = 0;
                    mentionError = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), botMissingAcccessList);
                } else {
                    i = 1;
                    mentionError = MentionUtil.getMentionedStringOfDiscriminatedUsers(getLocale(), memberMissingAccessList);
                }
                return EmbedFactory.getEmbedError(this,
                        TextManager.getString(getLocale(), Category.MODERATION, "warn_usererror_description", i, mentionError.getMentionText()),
                        TextManager.getString(getLocale(), TextManager.GENERAL, "wrong_args")
                );

            default:
                eb = getConfirmationEmbed();
                if (reason.length() > 0) {
                    eb.addField(TextManager.getString(getLocale(), Category.MODERATION, "warn_reason"), "```" + reason + "```", false);
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
