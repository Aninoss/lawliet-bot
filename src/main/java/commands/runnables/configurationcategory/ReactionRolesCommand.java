package commands.runnables.configurationcategory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.*;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
import core.cache.MessageCache;
import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.*;
import modules.ReactionRoles;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.reactionroles.DBReactionRoles;
import mysql.modules.reactionroles.ReactionRoleMessage;
import mysql.modules.reactionroles.ReactionRoleMessageSlot;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@CommandProperties(
        trigger = "reactionroles",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        botGuildPermissions = {Permission.MANAGE_ROLES, Permission.MESSAGE_HISTORY},
        userGuildPermissions = Permission.MANAGE_ROLES,
        emoji = "☑️️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"rmess", "reactionrole", "rroles", "selfrole", "selfroles", "sroles", "srole"}
)
public class ReactionRolesCommand extends NavigationAbstract implements OnReactionListener, OnStaticReactionAddListener, OnStaticReactionRemoveListener, OnStaticButtonListener, OnStaticStringSelectMenuListener {

    public final static int TITLE_LENGTH_MAX = 250;
    public final static int DESC_LENGTH_MAX = 1024;
    public final static int SLOTS_TEXT_LENGTH_MAX = 1024;
    public static final int MAX_REACTION_SLOTS = 20;
    public static final int MAX_SELECT_MENU_SLOTS = 24;
    public static final int MAX_BUTTON_SLOTS = 25;
    public static final int MAX_SLOTS_TOTAL = MAX_BUTTON_SLOTS;
    public static final int MAX_NEW_COMPONENTS_MESSAGES = 3;
    public static final int MAX_ROLE_REQUIREMENTS = 50;
    public static final int CUSTOM_LABEL_MAX_LENGTH = 100;

    private final static int
            ADD_OR_EDIT = 0,
            ADD_MESSAGE = 1,
            EDIT_MESSAGE = 2,
            CONFIGURE_MESSAGE = 3,
            UPDATE_TITLE = 4,
            UPDATE_DESC = 5,
            UPDATE_IMAGE = 10,
            ADD_SLOT = 6,
            REMOVE_SLOT = 7,
            ADD_ROLE_REQUIREMENT = 12,
            REMOVE_ROLE_REQUIREMENT = 13,
            UPDATE_COMPONENT_TYPE = 11,
            EXAMPLE = 8,
            SENT = 9;

    private String title;
    private String previousTitle;
    private String description;
    private List<ReactionRoleMessageSlot> slots = new ArrayList<>();
    private List<AtomicRole> roleRequirements = new ArrayList<>();
    private final NavigationHelper<AtomicRole> roleRequirementsNavigationHelper = new NavigationHelper<>(this, guildEntity -> roleRequirements, AtomicRole.class, MAX_ROLE_REQUIREMENTS, false);
    private Emoji emojiTemp;
    private String banner;
    private AtomicRole roleTemp;
    private String customLabelTemp;
    private AtomicTextChannel atomicTextChannel;
    private boolean removeRole = true;
    private boolean editMode = false;
    private boolean multipleRoles = true;
    private boolean showRoleConnections = true;
    private ReactionRoleMessage.ComponentType newComponents = ReactionRoleMessage.ComponentType.REACTIONS;
    private boolean showRoleNumbers = false;
    private long editMessageId = 0L;
    private File imageCdn = null;

    private static final Cache<Long, Boolean> BLOCK_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(30))
            .build();
    private static final Cache<Long, Boolean> USER_DM_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(15))
            .build();

    public ReactionRolesCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember());
        registerReactionListener(event.getMember());
        title = Command.getCommandLanguage(ReactionRolesCommand.class, getLocale()).getTitle();
        return true;
    }

    @ControllerMessage(state = ADD_MESSAGE)
    public MessageInputResponse onMessageAddMessage(MessageReceivedEvent event, String input) {
        List<TextChannel> serverTextChannel = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (!serverTextChannel.isEmpty()) {
            if (checkWriteEmbedInChannelWithLog(serverTextChannel.get(0))) {
                atomicTextChannel = new AtomicTextChannel(serverTextChannel.get(0));
                setLog(LogStatus.SUCCESS, getString("channelset"));
                return MessageInputResponse.SUCCESS;
            } else {
                return MessageInputResponse.FAILED;
            }
        }
        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    @ControllerMessage(state = UPDATE_TITLE)
    public MessageInputResponse onMessageUpdateTitle(MessageReceivedEvent event, String input) {
        if (!input.isEmpty() && input.length() <= TITLE_LENGTH_MAX) {
            title = input;
            setLog(LogStatus.SUCCESS, getString("titleset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(TITLE_LENGTH_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_DESC)
    public MessageInputResponse onMessageUpdateDesc(MessageReceivedEvent event, String input) {
        if (!input.isEmpty() && input.length() <= DESC_LENGTH_MAX) {
            description = input;
            setLog(LogStatus.SUCCESS, getString("descriptionset", input));
            setState(CONFIGURE_MESSAGE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(DESC_LENGTH_MAX)));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = UPDATE_IMAGE)
    public MessageInputResponse onMessageUpdateImage(MessageReceivedEvent event, String input) {
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        if (!attachments.isEmpty()) {
            Message.Attachment attachment = attachments.get(0);
            LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("reactionroles/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
            boolean success = FileUtil.downloadImageAttachment(attachment, tempFile);
            if (success) {
                banner = uploadFile(tempFile);
                setLog(LogStatus.SUCCESS, getString("imageset"));
                setState(CONFIGURE_MESSAGE);
                return MessageInputResponse.SUCCESS;
            }
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    private String uploadFile(LocalFile file) {
        if (imageCdn != null) {
            imageCdn.delete();
        }

        imageCdn = file;
        return file.cdnGetUrl();
    }

    @ControllerMessage(state = ADD_SLOT)
    public MessageInputResponse onMessageAddSlot(MessageReceivedEvent event, String input) {
        if (!input.isEmpty()) {
            boolean ok = false;
            List<Emoji> emojis = MentionUtil.getEmojis(event.getMessage(), input).getList();
            List<Role> roles = MentionUtil.getRoles(event.getGuild(), input).getList();

            if (!emojis.isEmpty()) {
                if (processEmoji(emojis.get(0))) {
                    ok = true;
                } else {
                    return MessageInputResponse.FAILED;
                }
            }

            if (!roles.isEmpty()) {
                if (processRole(event.getMember(), roles)) {
                    ok = true;
                } else {
                    return MessageInputResponse.FAILED;
                }
            }

            if (!ok) {
                if (input.length() <= CUSTOM_LABEL_MAX_LENGTH) {
                    this.customLabelTemp = input.replace("\n", " ");
                } else {
                    setLog(LogStatus.FAILURE, getString("customlabel_toomanychars", StringUtil.numToString(CUSTOM_LABEL_MAX_LENGTH)));
                    return MessageInputResponse.FAILED;
                }
            }

            return MessageInputResponse.SUCCESS;
        }

        setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
        return MessageInputResponse.FAILED;
    }

    @ControllerMessage(state = ADD_ROLE_REQUIREMENT)
    public MessageInputResponse onMessageAddRoleRequirement(MessageReceivedEvent event, String input) {
        List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
        return roleRequirementsNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), CONFIGURE_MESSAGE);
    }

    private boolean processEmoji(Emoji emoji) {
        if (emoji instanceof UnicodeEmoji || ShardManager.customEmojiIsKnown((CustomEmoji) emoji)) {
            this.emojiTemp = emoji;
            return true;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "emojiunknown", emoji.getName()));
            return false;
        }
    }

    private boolean processRole(Member member, List<Role> list) {
        Role roleTest = list.get(0);
        if (!checkRolesWithLog(member, List.of(roleTest))) {
            return false;
        }

        roleTemp = new AtomicRole(roleTest);
        return true;
    }

    @ControllerButton(state = ADD_OR_EDIT)
    public boolean onButtonAddOrEdit(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                setState(ADD_MESSAGE);
                editMode = false;
                editMessageId = 0L;
                return true;
            }
            case 1 -> {
                if (!ReactionRoles.getReactionMessagesInGuild(event.getGuild().getIdLong()).isEmpty()) {
                    setState(EDIT_MESSAGE);
                    editMode = true;
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, getString("noreactionmessage"));
                    return true;
                }
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = ADD_MESSAGE)
    public boolean onButtonAddMessage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                setState(ADD_OR_EDIT);
                return true;

            case 0:
                if (atomicTextChannel != null) {
                    setState(CONFIGURE_MESSAGE);
                    return true;
                }

            default:
                return false;
        }
    }

    @ControllerButton(state = EDIT_MESSAGE)
    public boolean onButtonEditMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(ADD_OR_EDIT);
            return true;
        } else if (i >= 0) {
            List<ReactionRoleMessage> reactionRoleMessages = ReactionRoles.getReactionMessagesInGuild(event.getGuild().getIdLong());
            if (i < reactionRoleMessages.size()) {
                ReactionRoleMessage reactionRoleMessage = reactionRoleMessages.get(i);
                StandardGuildMessageChannel channel = reactionRoleMessage.getStandardGuildMessageChannel().get();

                if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
                    String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_history", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
                    setLog(LogStatus.FAILURE, error);
                    return true;
                }

                try {
                    MessageCache.retrieveMessage(channel, reactionRoleMessage.getMessageId()).get();
                } catch (InterruptedException | ExecutionException e) {
                    // ignore
                    setLog(LogStatus.FAILURE, getString("messagedeleted"));
                    DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                            .remove(reactionRoleMessage.getMessageId());
                    return true;
                }

                editMessageId = reactionRoleMessage.getMessageId();
                updateValuesFromMessage(reactionRoleMessage);
                setState(CONFIGURE_MESSAGE);
                return true;
            }
        }

        return false;
    }

    @ControllerButton(state = CONFIGURE_MESSAGE)
    public boolean onButtonConfigureMessage(ButtonInteractionEvent event, int i) throws ExecutionException, InterruptedException, TimeoutException {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(ADD_MESSAGE);
                } else {
                    imageCdn = null;
                    setState(EDIT_MESSAGE);
                }
                return true;

            case 0:
                setState(UPDATE_TITLE);
                return true;

            case 1:
                setState(UPDATE_DESC);
                return true;

            case 2:
                setState(UPDATE_IMAGE);
                return true;

            case 3:
                if (slots.size() < MAX_SLOTS_TOTAL) {
                    setState(ADD_SLOT);
                } else {
                    setLog(LogStatus.FAILURE, getString("toomanyshortcuts", StringUtil.numToString(MAX_SLOTS_TOTAL)));
                }
                roleTemp = null;
                emojiTemp = null;
                customLabelTemp = null;
                return true;

            case 4:
                if (!slots.isEmpty()) {
                    setState(REMOVE_SLOT);
                } else {
                    setLog(LogStatus.FAILURE, getString("noshortcuts"));
                }
                return true;

            case 5:
                roleRequirementsNavigationHelper.startDataAdd(ADD_ROLE_REQUIREMENT);
                return true;

            case 6:
                roleRequirementsNavigationHelper.startDataRemove(REMOVE_ROLE_REQUIREMENT);
                return true;

            case 7:
                removeRole = !removeRole;
                setLog(LogStatus.SUCCESS, getString("roleremoveset"));
                return true;

            case 8:
                multipleRoles = !multipleRoles;
                setLog(LogStatus.SUCCESS, getString("multiplerolesset"));
                return true;

            case 9:
                showRoleConnections = !showRoleConnections;
                setLog(LogStatus.SUCCESS, getString("roleconnectionsset"));
                return true;

            case 10:
                setState(UPDATE_COMPONENT_TYPE);
                return true;

            case 11:
                showRoleNumbers = !showRoleNumbers;
                setLog(LogStatus.SUCCESS, getString("rolenumbersset"));
                return true;

            case 12:
                TextChannel textChannel = atomicTextChannel.get().orElse(null);
                String error = ReactionRoles.checkForErrors(getLocale(), textChannel, slots, roleRequirements, newComponents, editMessageId);
                if (error != null) {
                    setLog(LogStatus.FAILURE, error);
                    return true;
                }

                setState(EXAMPLE);
                return true;

            case 13:
                textChannel = atomicTextChannel.get().orElse(null);
                error = ReactionRoles.checkForErrors(getLocale(), textChannel, slots, roleRequirements, newComponents, editMessageId);
                if (error != null) {
                    setLog(LogStatus.FAILURE, error);
                    return true;
                }

                getEntityManager().getTransaction().begin();
                if (editMode) {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.REACTION_ROLES_EDIT, event.getMember(), previousTitle);
                } else {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.REACTION_ROLES_ADD, event.getMember(), title);
                }
                getEntityManager().getTransaction().commit();

                ReactionRoles.sendMessage(getLocale(), textChannel, title, description, slots, roleRequirements, removeRole,
                        multipleRoles, showRoleConnections, newComponents, showRoleNumbers, banner, editMode, editMessageId
                ).get(5, TimeUnit.SECONDS);

                setState(SENT);
                deregisterListeners();
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = UPDATE_IMAGE)
    public boolean onButtonUpdateImage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        } else if (i == 0) {
            if (imageCdn != null) {
                imageCdn.delete();
                imageCdn = null;
            }
            banner = null;
            setLog(LogStatus.SUCCESS, getString("imageset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        }

        return false;
    }

    @ControllerButton(state = ADD_SLOT)
    public boolean onButtonAddSlot(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1: {
                setState(CONFIGURE_MESSAGE);
                return true;
            }
            case 0: {
                emojiTemp = null;
                return true;
            }
            case 1: {
                customLabelTemp = null;
                return true;
            }
            case 2: {
                if (roleTemp != null) {
                    slots.add(new ReactionRoleMessageSlot(event.getGuild().getIdLong(), emojiTemp, roleTemp.getIdLong(), customLabelTemp));
                    setState(CONFIGURE_MESSAGE);
                    setLog(LogStatus.SUCCESS, getString("linkadded"));
                    return true;
                }
            }
        }

        return false;
    }

    @ControllerButton(state = UPDATE_COMPONENT_TYPE)
    public boolean onButtonUpdateComponentType(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        } else {
            newComponents = ReactionRoleMessage.ComponentType.values()[i];
            setLog(LogStatus.SUCCESS, getString("newcomponentsset"));
            setState(CONFIGURE_MESSAGE);
            return true;
        }
    }

    @ControllerButton(state = EXAMPLE)
    public boolean onButtonExample(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) throws Throwable {
        if (getState() == ADD_SLOT && event instanceof MessageReactionAddEvent) {
            processEmoji(event.getEmoji());
            processDraw(event.getMember(), true).exceptionally(ExceptionLogger.get());
            if (BotPermissionUtil.can(event.getGuildChannel(), Permission.MESSAGE_MANAGE)) {
                event.getReaction().removeReaction(event.getUser()).queue();
            }
            return false;
        }

        return false;
    }

    @ControllerButton(state = REMOVE_SLOT)
    public boolean onButtonRemoveSlot(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        if (i < slots.size() && i != -2) {
            slots.remove(i);
            setLog(LogStatus.SUCCESS, getString("linkremoved"));
            if (slots.size() == 0) {
                setState(CONFIGURE_MESSAGE);
            }
            return true;
        }
        return false;
    }

    @ControllerButton(state = ADD_ROLE_REQUIREMENT)
    public boolean onButtonAddRoleRequirement(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = REMOVE_ROLE_REQUIREMENT)
    public boolean onButtonRemoveRoleRequirement(ButtonInteractionEvent event, int i) {
        return roleRequirementsNavigationHelper.removeData(i, CONFIGURE_MESSAGE);
    }

    @ControllerButton(state = SENT)
    public boolean onButtonSent(ButtonInteractionEvent event, int i) {
        return false;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(CONFIGURE_MESSAGE);
            return true;
        }
        return false;
    }

    @Draw(state = ADD_OR_EDIT)
    public EmbedBuilder onDrawAddOrEdit(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"));
    }

    @Draw(state = ADD_MESSAGE)
    public EmbedBuilder onDrawAddMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (atomicTextChannel != null) {
            setComponents(TextManager.getString(getLocale(), TextManager.GENERAL, "continue"));
        }
        return EmbedFactory.getEmbedDefault(this, getString("state1_description", Optional.ofNullable(atomicTextChannel).map(m -> m.getPrefixedNameInField(getLocale())).orElse(notSet)), getString("state1_title"));
    }

    @Draw(state = EDIT_MESSAGE)
    public EmbedBuilder onDrawEditMessage(Member member) {
        List<ReactionRoleMessage> reactionRoleMessages = ReactionRoles.getReactionMessagesInGuild(member.getGuild().getIdLong());
        String[] options = new String[reactionRoleMessages.size()];
        for (int i = 0; i < reactionRoleMessages.size(); i++) {
            ReactionRoleMessage reactionRoleMessage = reactionRoleMessages.get(i);
            AtomicTextChannel channel = new AtomicTextChannel(reactionRoleMessage.getGuildId(), reactionRoleMessage.getStandardGuildMessageChannelId());
            options[i] = getString("state2_template", reactionRoleMessage.getTitle(), channel.getPrefixedName(getLocale()));
        }

        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = CONFIGURE_MESSAGE)
    public EmbedBuilder onDrawConfigureMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setComponents(
                getString(newComponents == ReactionRoleMessage.ComponentType.REACTIONS ? "state3_options" : "state3_options_newcomponents").split("\n"),
                new int[]{13},
                new int[0]
        );

        TextChannel textChannel = getTextChannel().get();
        String linkString = ReactionRoles.generateSlotOverview(slots, true, true);
        return EmbedFactory.getEmbedDefault(this, getString("state3_description", StringUtil.numToString(MAX_NEW_COMPONENTS_MESSAGES), ExternalLinks.PREMIUM_WEBSITE), getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(Optional.ofNullable(title).orElse(notSet)), true)
                .addField(getString("state3_mdescription"), StringUtil.shortenString(StringUtil.escapeMarkdown(Optional.ofNullable(description).orElse(notSet)), SLOTS_TEXT_LENGTH_MAX), true)
                .addField(getString("state3_mimage"), StringUtil.getOnOffForBoolean(textChannel, getLocale(), banner != null), true)
                .addField(getString("state3_mshortcuts"), StringUtil.shortenString(Optional.ofNullable(linkString).orElse(notSet), SLOTS_TEXT_LENGTH_MAX), true)
                .addField(getString("state3_mrolerequirements") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), new ListGen<AtomicRole>().getList(roleRequirements, getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("state3_mproperties"), getString(
                                newComponents == ReactionRoleMessage.ComponentType.REACTIONS ? "state3_mproperties_desc" : "state3_mproperties_desc_newcomponents",
                                StringUtil.getOnOffForBoolean(textChannel, getLocale(), removeRole),
                                StringUtil.getOnOffForBoolean(textChannel, getLocale(), multipleRoles),
                                StringUtil.getOnOffForBoolean(textChannel, getLocale(), showRoleConnections),
                                getString("componenttypes", newComponents.ordinal()),
                                StringUtil.getOnOffForBoolean(textChannel, getLocale(), showRoleNumbers)
                        ), false
                );
    }

    @Draw(state = UPDATE_TITLE)
    public EmbedBuilder onDrawUpdateTitle(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"));
    }

    @Draw(state = UPDATE_DESC)
    public EmbedBuilder onDrawUpdateDesc(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state5_description"), getString("state5_title"));
    }

    @Draw(state = UPDATE_IMAGE)
    public EmbedBuilder onDrawUpdateImage(Member member) {
        setComponents(getString("state10_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state10_description"), getString("state10_title"));
    }

    @Draw(state = ADD_SLOT)
    public EmbedBuilder onDrawAddSlot(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        ArrayList<String> options = new ArrayList<>();
        options.add(getString("state6_clearemoji"));
        options.add(getString("state6_clearcustomlabel"));
        if (roleTemp != null) {
            options.add(getString("state6_add"));
        }

        setComponents(options.toArray(new String[0]));
        return EmbedFactory.getEmbedDefault(this, getString(
                        "state6_description",
                        Optional.ofNullable(emojiTemp).map(Emoji::getFormatted).orElse(notSet),
                        Optional.ofNullable(roleTemp).map(m -> m.getPrefixedNameInField(getLocale())).orElse(notSet),
                        Emojis.COMMAND_ICON_PREMIUM.getFormatted(),
                        Optional.ofNullable(customLabelTemp).map(StringUtil::escapeMarkdown).orElse(notSet)
                ),
                getString("state6_title")
        );
    }

    @Draw(state = REMOVE_SLOT)
    public EmbedBuilder onDrawRemoveSlot(Member member) {
        ArrayList<Button> buttons = new ArrayList<>();
        ArrayList<ReactionRoleMessageSlot> tempSlots = new ArrayList<>(slots);
        for (int i = 0; i < tempSlots.size(); i++) {
            ReactionRoleMessageSlot slot = tempSlots.get(i);
            String roleName = new AtomicRole(getGuildId().get(), slot.getRoleId()).getPrefixedName(getLocale());
            String label = slot.getCustomLabel() != null
                    ? slot.getCustomLabel() + " (" + roleName + ")"
                    : roleName;
            Button button = Button.of(
                    ButtonStyle.PRIMARY,
                    String.valueOf(i),
                    StringUtil.shortenString(label, 80)
            );
            buttons.add(button);
        }
        setComponents(buttons);

        return EmbedFactory.getEmbedDefault(this, getString("state7_description"), getString("state7_title"));
    }

    @Draw(state = ADD_ROLE_REQUIREMENT)
    public EmbedBuilder onDrawAddRoleRequirement(Member member) {
        return roleRequirementsNavigationHelper.drawDataAdd(getString("state12_title"), getString("state12_description", ExternalLinks.PREMIUM_WEBSITE));
    }

    @Draw(state = REMOVE_ROLE_REQUIREMENT)
    public EmbedBuilder onDrawRemoveRoleRequirement(Member member) {
        return roleRequirementsNavigationHelper.drawDataRemove(getString("state13_title"), getLocale());
    }

    @Draw(state = UPDATE_COMPONENT_TYPE)
    public EmbedBuilder onDrawUpdateComponentType(Member member) {
        String[] options = new String[3];
        for (int i = 0; i < ReactionRoleMessage.ComponentType.values().length; i++) {
            options[i] = getString("componenttypes", i);
        }
        setComponents(options);

        return EmbedFactory.getEmbedDefault(this, getString("state11_description"), getString("state11_title"));
    }

    @Draw(state = EXAMPLE)
    public EmbedBuilder onDrawExample(Member member) throws ExecutionException, InterruptedException {
        setActionRows(ReactionRoles.getComponents(getLocale(), member.getGuild(), slots, removeRole, multipleRoles, newComponents, showRoleNumbers));
        return ReactionRoles.getMessageEmbed(getLocale(), member.getGuild().getIdLong(), title, description, slots,
                roleRequirements, showRoleConnections, banner);
    }

    @Draw(state = SENT)
    public EmbedBuilder onDrawSent(Member member) {
        return EmbedFactory.getEmbedDefault(this, getString("state9_description"), getString("state9_title"));
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }

        boolean premium = ServerPatreonBoostCache.get(event.getGuild().getIdLong());
        Member member = event.getMember();
        ReactionRoleMessage reactionRoleMessage = ReactionRoles.getReactionRoleMessage(message.getGuildChannel().asStandardGuildMessageChannel(), message.getIdLong());
        if (reactionRoleMessage == null ||
                reactionRoleMessage.getNewComponents() != ReactionRoleMessage.ComponentType.REACTIONS ||
                BLOCK_CACHE.asMap().containsKey(member.getIdLong())
        ) {
            return;
        }

        if (usesRoleRequirementsWithoutPremium(reactionRoleMessage, premium)) {
            sendUserErrorDm(event.getMember(), getString("components_result_nopro"));
            return;
        }

        if (usesNewComponentTypesWithoutPremium(reactionRoleMessage, premium)) {
            sendUserErrorDm(event.getMember(), getString("components_result_nopro_nonreactionsexceeded", StringUtil.numToString(MAX_NEW_COMPONENTS_MESSAGES)));
            return;
        }

        checkUsesCustomLabels(reactionRoleMessage, premium);

        if (violatesRoleRequirements(reactionRoleMessage, event.getMember())) {
            sendUserErrorDm(event.getMember(), getString("components_result_rolerequirements"));
            return;
        }

        GlobalThreadPool.submit(() -> {
            try {
                if (!reactionRoleMessage.getMultipleRoles()) {
                    BLOCK_CACHE.put(member.getIdLong(), true);
                    if (removeMultipleRoles(event.getMember(), reactionRoleMessage, new ArrayList<>(), new ArrayList<>())) {
                        sendUserErrorDm(event.getMember(), getString("components_result_noremoval"));
                        return;
                    }
                }

                for (ReactionRoleMessageSlot slot : new ArrayList<>(reactionRoleMessage.getSlots())) {
                    if (EmojiUtil.equals(event.getEmoji(), slot.getEmoji())) {
                        Role role = event.getGuild().getRoleById(slot.getRoleId());
                        if (role == null) {
                            sendUserErrorDm(event.getMember(), getString("components_result_norole"));
                            return;
                        }

                        addRoleToMember(member, role);
                        return;
                    }
                }
            } finally {
                if (!reactionRoleMessage.getMultipleRoles()) {
                    BLOCK_CACHE.invalidate(member.getIdLong());
                }
            }
        });
    }

    @Override
    public void onStaticReactionRemove(@NotNull Message message, @NotNull MessageReactionRemoveEvent event) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }
        boolean premium = ServerPatreonBoostCache.get(event.getGuild().getIdLong());

        ReactionRoleMessage reactionRoleMessage = ReactionRoles.getReactionRoleMessage(message.getGuildChannel().asStandardGuildMessageChannel(), message.getIdLong());
        if (reactionRoleMessage == null ||
                reactionRoleMessage.getNewComponents() != ReactionRoleMessage.ComponentType.REACTIONS
        ) {
            return;
        }

        if (!reactionRoleMessage.getRoleRemoval()) {
            sendUserErrorDm(event.getMember(), getString("components_result_noremoval"));
            return;
        }

        if (usesRoleRequirementsWithoutPremium(reactionRoleMessage, premium)) {
            sendUserErrorDm(event.getMember(), getString("components_result_nopro"));
            return;
        }

        if (usesNewComponentTypesWithoutPremium(reactionRoleMessage, premium)) {
            sendUserErrorDm(event.getMember(), getString("components_result_nopro_nonreactionsexceeded", StringUtil.numToString(MAX_NEW_COMPONENTS_MESSAGES)));
            return;
        }

        checkUsesCustomLabels(reactionRoleMessage, premium);

        if (violatesRoleRequirements(reactionRoleMessage, event.getMember())) {
            sendUserErrorDm(event.getMember(), getString("components_result_rolerequirements"));
            return;
        }

        for (ReactionRoleMessageSlot slot : new ArrayList<>(reactionRoleMessage.getSlots())) {
            if (EmojiUtil.equals(event.getEmoji(), slot.getEmoji())) {
                Role role = event.getGuild().getRoleById(slot.getRoleId());
                if (role == null) {
                    sendUserErrorDm(event.getMember(), getString("components_result_norole"));
                    return;
                }

                removeRoleFromMember(event.getMember(), role);
                break;
            }
        }
    }

    @Override
    public void onStaticButton(@NotNull ButtonInteractionEvent event, @Nullable String secondaryId) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }
        boolean premium = ServerPatreonBoostCache.get(event.getGuild().getIdLong());

        Member member = event.getMember();
        ReactionRoleMessage reactionRoleMessage = ReactionRoles.getReactionRoleMessage(event.getGuildChannel().asStandardGuildMessageChannel(), event.getMessageIdLong());
        if (reactionRoleMessage == null ||
                BLOCK_CACHE.asMap().containsKey(member.getIdLong())
        ) {
            return;
        }

        if (usesRoleRequirementsWithoutPremium(reactionRoleMessage, premium)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_nopro")).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (usesNewComponentTypesWithoutPremium(reactionRoleMessage, premium)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_nopro_nonreactionsexceeded", StringUtil.numToString(MAX_NEW_COMPONENTS_MESSAGES))).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        checkUsesCustomLabels(reactionRoleMessage, premium);

        if (violatesRoleRequirements(reactionRoleMessage, member)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_rolerequirements")).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferEdit().queue();
        GlobalThreadPool.submit(() -> {
            try {
                ReactionRoleMessageSlot slot = reactionRoleMessage.getSlots().get(Integer.parseInt(event.getComponentId()));
                Role role = slot.getRole().orElse(null);
                if (role == null) {
                    event.getHook()
                            .sendMessageEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_norole")).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                if (member.getRoles().contains(role)) {
                    if (reactionRoleMessage.getRoleRemoval()) {
                        if (removeRoleFromMember(member, role)) {
                            if (reactionRoleMessage.getShowRoleNumbers()) {
                                Thread.sleep(100);
                                event.getHook().editOriginalComponents(
                                        ReactionRoles.getComponents(getLocale(), event.getGuild(), reactionRoleMessage.getSlots(),
                                                reactionRoleMessage.getRoleRemoval(), reactionRoleMessage.getMultipleRoles(),
                                                reactionRoleMessage.getNewComponents(), reactionRoleMessage.getShowRoleNumbers()
                                        )
                                ).queue();
                            }
                            event.getHook()
                                    .sendMessageEmbeds(generateRoleSummary(Collections.emptyList(), List.of(role), Collections.emptyList()).build())
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        } else {
                            event.getHook()
                                    .sendMessageEmbeds(generateRoleSummary(Collections.emptyList(), Collections.emptyList(), List.of(role)).build())
                                    .setEphemeral(true)
                                    .queue();
                            return;
                        }
                    } else {
                        event.getHook()
                                .sendMessageEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_noremoval")).build())
                                .setEphemeral(true)
                                .queue();
                        return;
                    }
                }

                ArrayList<Role> addedRoles = new ArrayList<>();
                ArrayList<Role> removedRoles = new ArrayList<>();
                ArrayList<Role> unmanageableRoles = new ArrayList<>();

                if (!reactionRoleMessage.getMultipleRoles()) {
                    BLOCK_CACHE.put(member.getIdLong(), true);
                    if (removeMultipleRoles(event.getMember(), reactionRoleMessage, removedRoles, unmanageableRoles)) {
                        event.getHook()
                                .sendMessageEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_onlyone")).build())
                                .setEphemeral(true)
                                .queue();
                        return;
                    }
                }

                if (addRoleToMember(member, role)) {
                    addedRoles.add(role);
                } else {
                    unmanageableRoles.add(role);
                }

                if (reactionRoleMessage.getShowRoleNumbers() && (!addedRoles.isEmpty() || !removedRoles.isEmpty())) {
                    Thread.sleep(100);
                    event.getHook().editOriginalComponents(
                            ReactionRoles.getComponents(getLocale(), event.getGuild(), reactionRoleMessage.getSlots(),
                                    reactionRoleMessage.getRoleRemoval(), reactionRoleMessage.getMultipleRoles(),
                                    reactionRoleMessage.getNewComponents(), reactionRoleMessage.getShowRoleNumbers()
                            )
                    ).queue();
                }

                event.getHook()
                        .sendMessageEmbeds(generateRoleSummary(addedRoles, removedRoles, unmanageableRoles).build())
                        .setEphemeral(true)
                        .queue();
            } catch (ExecutionException | InterruptedException e) {
                MainLogger.get().error("Error in reaction roles component update", e);
            } finally {
                if (!reactionRoleMessage.getMultipleRoles()) {
                    BLOCK_CACHE.invalidate(member.getIdLong());
                }
            }
        });
    }

    @Override
    public void onStaticStringSelectMenu(@NotNull StringSelectInteractionEvent event, @Nullable String secondaryId) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }
        boolean premium = ServerPatreonBoostCache.get(event.getGuild().getIdLong());

        Member member = event.getMember();
        ReactionRoleMessage reactionRoleMessage = ReactionRoles.getReactionRoleMessage(event.getGuildChannel().asStandardGuildMessageChannel(), event.getMessageIdLong());
        if (reactionRoleMessage == null ||
                BLOCK_CACHE.asMap().containsKey(member.getIdLong())
        ) {
            return;
        }

        if (usesRoleRequirementsWithoutPremium(reactionRoleMessage, premium)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_nopro")).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (usesNewComponentTypesWithoutPremium(reactionRoleMessage, premium)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_nopro_nonreactionsexceeded", StringUtil.numToString(MAX_NEW_COMPONENTS_MESSAGES))).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        checkUsesCustomLabels(reactionRoleMessage, premium);

        if (violatesRoleRequirements(reactionRoleMessage, member)) {
            event.replyEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_rolerequirements")).build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferEdit().queue();
        GlobalThreadPool.submit(() -> {
            try {
                HashSet<Role> checkedRoles = new HashSet<>();
                ArrayList<Role> addedRoles = new ArrayList<>();
                ArrayList<Role> unmanageableRoles = new ArrayList<>();
                ArrayList<Role> removedRoles = new ArrayList<>();

                for (String value : event.getValues()) {
                    int slotId = Integer.parseInt(value);
                    if (slotId >= 0) {
                        ReactionRoleMessageSlot slot = reactionRoleMessage.getSlots().get(slotId);
                        Role role = slot.getRole().orElse(null);
                        if (role == null ||
                                checkedRoles.contains(role)
                        ) {
                            continue;
                        }

                        checkedRoles.add(role);
                        if (member.getRoles().contains(role)) {
                            continue;
                        }

                        if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), role)) {
                            addedRoles.add(role);
                        } else {
                            unmanageableRoles.add(role);
                        }
                    }
                }

                boolean roleRemoved = false;
                for (ReactionRoleMessageSlot slot : reactionRoleMessage.getSlots()) {
                    Role role = slot.getRole().orElse(null);
                    if (role == null ||
                            checkedRoles.contains(role)
                    ) {
                        continue;
                    }

                    checkedRoles.add(role);
                    if (!member.getRoles().contains(role)) {
                        continue;
                    }

                    roleRemoved = true;
                    if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), role)) {
                        removedRoles.add(role);
                    } else {
                        unmanageableRoles.add(role);
                    }
                }

                if (roleRemoved && !reactionRoleMessage.getRoleRemoval()) {
                    if (reactionRoleMessage.getShowRoleNumbers()) {
                        event.getHook().editOriginalComponents(
                                ReactionRoles.getComponents(getLocale(), event.getGuild(), reactionRoleMessage.getSlots(),
                                        reactionRoleMessage.getRoleRemoval(), reactionRoleMessage.getMultipleRoles(),
                                        reactionRoleMessage.getNewComponents(), reactionRoleMessage.getShowRoleNumbers()
                                )
                        ).queue();
                    }
                    event.getHook()
                            .sendMessageEmbeds(EmbedFactory.getEmbedError(this, getString("components_result_noremoval")).build())
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                ArrayList<Role> memberRoles = new ArrayList<>(member.getRoles());
                memberRoles.removeAll(removedRoles);
                memberRoles.addAll(addedRoles);
                if (!addedRoles.isEmpty() || !removedRoles.isEmpty()) {
                    member.getGuild().modifyMemberRoles(member, memberRoles)
                            .reason(getCommandLanguage().getTitle())
                            .complete();
                }

                if (reactionRoleMessage.getShowRoleNumbers()) {
                    Thread.sleep(100);
                    event.getHook().editOriginalComponents(
                            ReactionRoles.getComponents(getLocale(), event.getGuild(), reactionRoleMessage.getSlots(),
                                    reactionRoleMessage.getRoleRemoval(), reactionRoleMessage.getMultipleRoles(),
                                    reactionRoleMessage.getNewComponents(), reactionRoleMessage.getShowRoleNumbers()
                            )
                    ).queue();
                }
                event.getHook()
                        .sendMessageEmbeds(generateRoleSummary(addedRoles, removedRoles, unmanageableRoles).build())
                        .setEphemeral(true)
                        .queue();
            } catch (ExecutionException | InterruptedException e) {
                MainLogger.get().error("Error in reaction roles component update", e);
            }
        });
    }

    private void updateValuesFromMessage(ReactionRoleMessage message) {
        this.title = message.getTitle();
        this.previousTitle = this.title;
        this.description = message.getDesc();
        this.banner = message.getImage();
        this.multipleRoles = message.getMultipleRoles();
        this.removeRole = message.getRoleRemoval();
        this.showRoleConnections = message.getShowRoleConnections();
        this.newComponents = message.getNewComponents();
        this.showRoleNumbers = message.getShowRoleNumbers();
        this.slots = new ArrayList<>(message.getSlots());
        this.roleRequirements = new ArrayList<>(message.getRoleRequirements());
        this.atomicTextChannel = new AtomicTextChannel(message.getGuildId(), message.getStandardGuildMessageChannelId());
    }

    private boolean removeMultipleRoles(Member member, ReactionRoleMessage reactionRoleMessage,
                                        ArrayList<Role> removedRoles, ArrayList<Role> unmanageableRoles) {
        for (ReactionRoleMessageSlot slot : reactionRoleMessage.getSlots()) {
            Role role = member.getGuild().getRoleById(slot.getRoleId());
            if (role != null) {
                if (member.getRoles().contains(role)) {
                    if (!reactionRoleMessage.getRoleRemoval()) {
                        return true;
                    }
                    if (removeRoleFromMember(member, role)) {
                        removedRoles.add(role);
                    } else {
                        unmanageableRoles.add(role);
                    }
                }
            }
        }

        return false;
    }

    private boolean addRoleToMember(Member member, Role role) {
        if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), role)) {
            member.getGuild().addRoleToMember(member, role)
                    .reason(getCommandLanguage().getTitle())
                    .complete();
            return true;
        }
        return false;
    }

    private boolean removeRoleFromMember(Member member, Role role) {
        if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), role)) {
            member.getGuild().removeRoleFromMember(member, role)
                    .reason(getCommandLanguage().getTitle())
                    .complete();
            return true;
        }
        return false;
    }

    private EmbedBuilder generateRoleSummary(List<Role> addedRoles, List<Role> removedRoles, List<Role> unmanageableRoles) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        if (addedRoles.isEmpty() && removedRoles.isEmpty()) {
            eb.setDescription(getString("components_result_noupdates"));
        } else {
            if (!removedRoles.isEmpty()) {
                eb.addField(getString("components_result_removedroles"), new ListGen<Role>().getList(removedRoles, r -> new AtomicRole(r).getPrefixedNameInField(getLocale())), true);
            }
            if (!addedRoles.isEmpty()) {
                eb.addField(getString("components_result_newroles"), new ListGen<Role>().getList(addedRoles, r -> new AtomicRole(r).getPrefixedNameInField(getLocale())), true);
            }
        }

        if (!unmanageableRoles.isEmpty()) {
            EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanageableRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), unmanageableRoles).getMentionText().replace("**", "\"")));
        }

        return eb;
    }

    private void sendUserErrorDm(Member member, String message) {
        if (USER_DM_CACHE.getIfPresent(member.getIdLong()) == null) {
            USER_DM_CACHE.put(member.getIdLong(), true);
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, message);
            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();
        }
    }

    private boolean violatesRoleRequirements(ReactionRoleMessage reactionRoleMessage, Member member) {
        return !reactionRoleMessage.getRoleRequirements().isEmpty() &&
                reactionRoleMessage.getRoleRequirements().stream().noneMatch(atomicRole -> member.getRoles().stream().anyMatch(r -> r.getIdLong() == atomicRole.getIdLong()));
    }

    private void checkUsesCustomLabels(ReactionRoleMessage reactionRoleMessage, boolean premium) {
        if (premium && reactionRoleMessage.getSlots().stream().anyMatch(slot -> slot.getCustomLabel() != null)) {
            FeatureLogger.inc(PremiumFeature.REACTION_ROLES_CUSTOM_LABEL, reactionRoleMessage.getGuildId());
        }
    }

    private boolean usesRoleRequirementsWithoutPremium(ReactionRoleMessage reactionRoleMessage, boolean premium) {
        if (!reactionRoleMessage.getRoleRequirements().isEmpty()) {
            if (premium) {
                FeatureLogger.inc(PremiumFeature.REACTION_ROLES_ROLE_REQUIREMENTS, reactionRoleMessage.getGuildId());
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean usesNewComponentTypesWithoutPremium(ReactionRoleMessage reactionRoleMessage, boolean premium) {
        if (reactionRoleMessage.getNewComponents() == ReactionRoleMessage.ComponentType.REACTIONS) {
            return false;
        }

        int newComponentTypeMessages = (int) DBReactionRoles.getInstance().retrieve(reactionRoleMessage.getGuildId()).values().stream()
                .filter(r -> r.getNewComponents() != ReactionRoleMessage.ComponentType.REACTIONS)
                .count();
        if (newComponentTypeMessages > ReactionRolesCommand.MAX_NEW_COMPONENTS_MESSAGES) {
            if (premium) {
                FeatureLogger.inc(PremiumFeature.REACTION_ROLES_NEW_COMPONENTS_LIMIT, reactionRoleMessage.getGuildId());
            } else {
                return true;
            }
        }
        return false;
    }

}
