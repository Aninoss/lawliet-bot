package commands.runnables.moderationcategory;

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
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.ISnowflake;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "warn",
        userGuildPermissions = Permission.KICK_MEMBERS,
        emoji = "\uD83D\uDEA8",
        executableWithoutArgs = false,
        requiresFullMemberCache = true
)
public class WarnCommand extends Command implements OnButtonListener {

    private enum Status {PENDING, CANCELED, COMPLETED, ERROR}

    public static final int REASON_MAX = 300;

    private List<User> userList;
    private String reason;
    private Status status = Status.PENDING;
    private final ArrayList<User> memberMissingAccessList = new ArrayList<>();
    private final ArrayList<User> botMissingAccessList = new ArrayList<>();
    private final boolean sendWarning;
    private final boolean autoActions;
    private final boolean includeNotInGuild;
    private final boolean sendLogWarnings;
    private final boolean hasDuration;
    private final boolean includeBanAppealButton;

    public WarnCommand(Locale locale, String prefix) {
        this(locale, prefix, true, true, true, true, false, false);
    }

    public WarnCommand(Locale locale, String prefix, boolean sendWarning, boolean autoActions, boolean includeNotInGuild,
                       boolean sendLogWarnings, boolean hasDuration, boolean includeBanAppealButton
    ) {
        super(locale, prefix);
        this.sendWarning = sendWarning;
        this.autoActions = autoActions;
        this.includeNotInGuild = includeNotInGuild;
        this.sendLogWarnings = sendLogWarnings;
        this.hasDuration = hasDuration;
        this.includeBanAppealButton = includeBanAppealButton;
    }

    protected MentionList<User> getUserList(CommandEvent event, String args) throws Throwable {
        Guild guild = event.getGuild();
        if (includeNotInGuild || !guild.isLoaded()) {
            deferReply();
        }

        MentionList<Member> memberMention = MentionUtil.getMembers(guild, args, event.getRepliedMember());
        List<User> userList = memberMention.getList().stream()
                .map(Member::getUser)
                .collect(Collectors.toList());

        if (includeNotInGuild) {
            MentionList<User> userMention = MentionUtil.getUsersFromString(args, false).get();
            if (!userMention.getList().isEmpty()) {
                return userMention;
            }
        }
        return new MentionList<>(memberMention.getFilteredArgs(), userList);
    }

    public void userActionPrepareExecution(User target, String reason, long durationMinutes, int amount) {
        this.userList = List.of(target);
        this.reason = reason;
    }

    public EmbedBuilder userActionCheckGeneralError() {
        return null;
    }

    protected void process(Guild guild, User target, String reason) throws Throwable {
    }

    protected void processAll(Guild guild, List<User> targets, String reason) throws Throwable {
    }

    public boolean canProcessMember(Member executor, User target) throws Throwable {
        return BotPermissionUtil.canInteract(executor, target);
    }

    public boolean canProcessBot(Guild guild, User target) throws Throwable {
        return BotPermissionUtil.canInteract(guild, target);
    }

    public boolean hasDuration() {
        return hasDuration;
    }

    public boolean getIncludeNotInGuild() {
        return includeNotInGuild;
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        if (!setUserListAndReason(event, args)) {
            return false;
        }

        checkAllProcess(event.getMember());
        if (!memberMissingAccessList.isEmpty() || !botMissingAccessList.isEmpty()) {
            status = Status.ERROR;
            drawMessage(draw(event.getMember())).exceptionally(ExceptionLogger.get());
            return false;
        }

        if (userList.size() > 1 || getGuildEntity().getModeration().getConfirmationMessages()) {
            setComponents(
                    Button.of(ButtonStyle.SUCCESS, "true", TextManager.getString(getLocale(), Category.MODERATION, "warn_button_confirm")),
                    Button.of(ButtonStyle.SECONDARY, "false", TextManager.getString(getLocale(), TextManager.GENERAL, "process_abort"))
            );
            registerButtonListener(event.getMember());
        } else {
            deferReply();
            boolean success = checkAndExecute(event.getMessageChannel(), event.getMember());
            drawMessage(draw(event.getMember())).exceptionally(ExceptionLogger.get());
            return success;
        }

        return true;
    }

    private void checkAllProcess(Member executor) throws Throwable {
        for (User user : userList) {
            if (!canProcessBot(executor.getGuild(), user)) {
                botMissingAccessList.add(user);
            } else if (!canProcessMember(executor, user)) {
                memberMissingAccessList.add(user);
            }
        }
    }

    protected boolean setUserListAndReason(CommandEvent event, String args) throws Throwable {
        MentionList<User> userMention = getUserList(event, args);
        userList = userMention.getList();
        if (userList.isEmpty()) {
            drawMessageNew(getNoMentionEmbed())
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        reason = userMention.getFilteredArgs().trim();
        reason = JDAUtil.resolveMentions(event.getGuild(), reason);
        reason = StringUtil.shortenString(reason, REASON_MAX);

        return true;
    }

    public boolean checkAndExecute(GuildChannel channel, Member executor) throws Throwable {
        checkAllProcess(executor);
        if (!memberMissingAccessList.isEmpty() || !botMissingAccessList.isEmpty()) {
            status = Status.ERROR;
            return false;
        }

        EmbedBuilder actionEmbed = getActionEmbed(executor, channel);
        if (!reason.isEmpty()) {
            actionEmbed.addField(TextManager.getString(getLocale(), Category.MODERATION, "warn_reason"), "```" + reason + "```", false);
        }

        if (sendLogWarnings) {
            Mod.postLogUsers(this, actionEmbed, channel.getGuild(), getGuildEntity().getModeration(), userList, includeBanAppealButton).join();
        } else {
            Mod.sendAnnouncement(this, actionEmbed, getGuildEntity().getModeration());
        }

        for (User user : userList) {
            if (sendWarning) {
                Mod.insertWarning(getGuildEntity(), channel.getGuild(), user, executor, reason, autoActions);
            }
            process(channel.getGuild(), user, reason);
        }
        processAll(channel.getGuild(), userList, reason);

        EntityManagerWrapper entityManager = getEntityManager();
        entityManager.getTransaction().begin();
        BotLogEntity.log(entityManager, getBotLogEvent(), executor, null, null, userList.stream().map(ISnowflake::getIdLong).collect(Collectors.toList()));
        entityManager.getTransaction().commit();

        status = Status.COMPLETED;
        return true;
    }

    protected EmbedBuilder getActionEmbed(Member executor, GuildChannel channel) {
        Mention mention = MentionUtil.getMentionedStringOfUsernames(getLocale(), userList);
        return EmbedFactory.getEmbedDefault(this, getString("action", mention.isMultiple(), mention.getMentionText(), StringUtil.escapeMarkdown(executor.getUser().getName()), StringUtil.escapeMarkdown(channel.getGuild().getName())));
    }

    protected EmbedBuilder getConfirmationEmbed() {
        Mention mention = MentionUtil.getMentionedStringOfUsernames(getLocale(), userList);
        return EmbedFactory.getEmbedDefault(this, getString("confirmaion", mention.getMentionText()));
    }

    protected EmbedBuilder getSuccessEmbed() {
        Mention mention = MentionUtil.getMentionedStringOfUsernames(getLocale(), userList);
        return EmbedFactory.getEmbedDefault(this, getString("success_description", mention.isMultiple(), mention.getMentionText()));
    }

    protected EmbedBuilder getNoMentionEmbed() {
        return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_mentions"));
    }

    protected BotLogEntity.Event getBotLogEvent() {
        return BotLogEntity.Event.MOD_WARN;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        deregisterListenersWithComponents();
        boolean confirm = Boolean.parseBoolean(event.getComponentId());
        if (confirm) {
            event.deferEdit().queue();
            checkAndExecute(event.getGuildChannel(), event.getMember());
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
                if (!reason.isEmpty()) {
                    eb.addField(TextManager.getString(getLocale(), Category.MODERATION, "warn_reason"), "```" + StringUtil.escapeMarkdownInField(reason) + "```", false);
                }
                return eb;

            case CANCELED:
                return EmbedFactory.getAbortEmbed(this);

            case ERROR:
                Mention mentionError;
                int i;
                if (!botMissingAccessList.isEmpty()) {
                    i = 0;
                    mentionError = MentionUtil.getMentionedStringOfUsernames(getLocale(), botMissingAccessList);
                } else {
                    i = 1;
                    mentionError = MentionUtil.getMentionedStringOfUsernames(getLocale(), memberMissingAccessList);
                }
                return EmbedFactory.getEmbedError(this,
                        TextManager.getString(getLocale(), Category.MODERATION, i == 0 ? "warn_rolepos_bot" : "warn_rolepos_user", mentionError.getMentionText()),
                        TextManager.getString(getLocale(), TextManager.GENERAL, "wrong_args")
                );

            default:
                eb = getConfirmationEmbed();
                if (!reason.isEmpty()) {
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
