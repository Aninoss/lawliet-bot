package commands.runnables.configurationcategory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import commands.Command;
import commands.CommandEvent;
import commands.listeners.*;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.EmojiStateProcessor;
import commands.stateprocessor.FileStateProcessor;
import commands.stateprocessor.RolesStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicRole;
import core.cache.MessageCache;
import core.cache.ServerPatreonBoostCache;
import core.collectionadapters.ListAdapter;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.modals.StringModalBuilder;
import core.utils.*;
import modules.ReactionRoles;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.ReactionRoleEntity;
import mysql.hibernate.entity.ReactionRoleSlotEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
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

    public final static int TITLE_LENGTH_MAX = MessageEmbed.TITLE_MAX_LENGTH;
    public final static int DESC_LENGTH_MAX = MessageEmbed.VALUE_MAX_LENGTH;
    public final static int SLOTS_TEXT_LENGTH_MAX = MessageEmbed.VALUE_MAX_LENGTH;
    public static final int MAX_REACTION_SLOTS = Message.MAX_REACTIONS;
    public static final int MAX_SELECT_MENU_SLOTS = StringSelectMenu.OPTIONS_MAX_AMOUNT - 1;
    public static final int MAX_SLOTS_TOTAL = 25;
    public static final int MAX_ROLE_MESSAGES_FREE = 10;
    public static final int MAX_ROLE_REQUIREMENTS = EntitySelectMenu.OPTIONS_MAX_AMOUNT;
    public static final int MAX_ROLES = EntitySelectMenu.OPTIONS_MAX_AMOUNT;
    public static final int CUSTOM_LABEL_MAX_LENGTH = 100;

    private final static int
            STATE_SET_CHANNEL = 1,
            STATE_EDIT = 2,
            STATE_CONFIG = 3,
            STATE_SET_DESC = 4,
            STATE_SET_IMAGE = 5,
            STATE_ADD_SLOT = 6,
            STATE_ADD_SLOT_SET_EMOJI = 7,
            STATE_ADD_SLOT_SET_ROLES = 8,
            STATE_REMOVE_SLOTS = 9,
            STATE_SET_ROLE_REQUIREMENTS = 10,
            STATE_UPDATE_COMPONENT_TYPE = 11,
            STATE_EXAMPLE = 12;

    private EmojiStateProcessor emojiStateProcessor;
    private ReactionRoleEntity configuration;
    private String previousTitle;
    private ReactionRoleSlotEntity slotConfiguration;
    private boolean editMode = false;
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
        resetRoleMessage(event.getGuild().getIdLong());

        emojiStateProcessor = new EmojiStateProcessor(this, STATE_ADD_SLOT_SET_EMOJI, STATE_ADD_SLOT, getString("addslot_emoji"))
                .setClearButton(true)
                .setSetter(emoji -> {
                    if (emoji != null) {
                        slotConfiguration.setEmojiFormatted(emoji.getFormatted());
                    } else {
                        slotConfiguration.setEmojiFormatted(null);
                    }
                });

        registerNavigationListener(event.getMember(), List.of(
                new StringStateProcessor(this, STATE_SET_DESC, STATE_CONFIG, getString("state3_mdescription"))
                        .setMax(DESC_LENGTH_MAX)
                        .setClearButton(true)
                        .setSetter(s -> configuration.setDescription(s)),
                new FileStateProcessor(this, STATE_SET_IMAGE, STATE_CONFIG, getString("dashboard_includedimage"))
                        .setClearButton(true)
                        .setSetter(attachment -> {
                            if (attachment != null) {
                                LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("reactionroles/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                                if (!FileUtil.downloadImageAttachment(attachment, tempFile)) {
                                    throw new RuntimeException("File download failed");
                                }
                                configuration.setImageUrl(uploadFile(tempFile));
                            } else {
                                deleteTemporaryImage();
                                configuration.setImageFilename(null);
                            }
                        }),
                new RolesStateProcessor(this, STATE_SET_ROLE_REQUIREMENTS, STATE_CONFIG, getString("state3_mrolerequirements"))
                        .setMinMax(0, MAX_ROLE_REQUIREMENTS)
                        .setCheckAccess(false)
                        .setGetter(() -> configuration.getRoleRequirementIds())
                        .setSetter(roleIds -> configuration.setRoleRequirementIds(roleIds)),
                emojiStateProcessor,
                new RolesStateProcessor(this, STATE_ADD_SLOT_SET_ROLES, STATE_ADD_SLOT, getString("addslot_roles"))
                        .setMinMax(1, MAX_ROLES)
                        .setCheckAccess(true)
                        .setGetter(() -> slotConfiguration.getRoleIds())
                        .setSetter(roleIds -> slotConfiguration.setRoleIds(roleIds))
        ));
        registerReactionListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonAddOrEdit(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deleteTemporaryImage();
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                if (!ServerPatreonBoostCache.get(event.getGuild().getIdLong()) && getGuildEntity().getReactionRoles().size() >= MAX_ROLE_MESSAGES_FREE) {
                    setLog(LogStatus.FAILURE, getString("limitexceeded"));
                    return true;
                }
                editMode = false;
                setState(STATE_SET_CHANNEL);
                return true;
            }
            case 1 -> {
                if (!getGuildEntity().getReactionRoles().isEmpty()) {
                    setState(STATE_EDIT);
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

    @ControllerButton(state = STATE_SET_CHANNEL)
    public boolean onButtonSetChannel(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerButton(state = STATE_EDIT)
    public boolean onButtonEditMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        List<ReactionRoleEntity> reactionRoles = new ArrayList<>(getGuildEntity().getReactionRoles().values());
        if (i >= reactionRoles.size()) {
            return false;
        }

        ReactionRoleEntity reactionRoleEntity = reactionRoles.get(i);
        GuildMessageChannel channel = reactionRoleEntity.getMessageChannel().get().orElse(null);
        if (channel == null) {
            setLog(LogStatus.FAILURE, getString("messagedeleted"));
            DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                    .remove(reactionRoleEntity.getMessageId());
            return true;
        }

        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY)) {
            String error = TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_history", "#" + StringUtil.escapeMarkdownInField(channel.getName()));
            setLog(LogStatus.FAILURE, error);
            return true;
        }

        try {
            MessageCache.retrieveMessage(channel, reactionRoleEntity.getMessageId()).get();
        } catch (InterruptedException | ExecutionException e) {
            // ignore
            setLog(LogStatus.FAILURE, getString("messagedeleted"));
            DBStaticReactionMessages.getInstance().retrieve(event.getGuild().getIdLong())
                    .remove(reactionRoleEntity.getMessageId());
            return true;
        }

        configuration = reactionRoleEntity.copy();
        previousTitle = configuration.getTitle();
        deleteTemporaryImage();
        setState(STATE_CONFIG);
        return true;
    }

    @ControllerButton(state = STATE_CONFIG)
    public boolean onButtonConfigureMessage(ButtonInteractionEvent event, int i) throws ExecutionException, InterruptedException, TimeoutException {
        switch (i) {
            case -1:
                if (!editMode) {
                    setState(STATE_SET_CHANNEL);
                } else {
                    resetRoleMessage(event.getGuild().getIdLong());
                    deleteTemporaryImage();
                    setState(STATE_EDIT);
                }
                return true;

            case 0:
                Modal modal = new StringModalBuilder(this, getString("state3_mtitle"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, TITLE_LENGTH_MAX)
                        .setGetter(() -> configuration.getTitle())
                        .setSetter(s -> configuration.setTitle(s))
                        .build();
                event.replyModal(modal).queue();
                return false;

            case 1:
                setState(STATE_SET_DESC);
                return true;

            case 2:
                setState(STATE_SET_IMAGE);
                return true;

            case 3:
                if (configuration.getSlots().size() < MAX_SLOTS_TOTAL) {
                    slotConfiguration = new ReactionRoleSlotEntity();
                    setState(STATE_ADD_SLOT);
                } else {
                    setLog(LogStatus.FAILURE, getString("toomanyshortcuts", StringUtil.numToString(MAX_SLOTS_TOTAL)));
                }
                return true;

            case 4:
                if (!configuration.getSlots().isEmpty()) {
                    setState(STATE_REMOVE_SLOTS);
                } else {
                    setLog(LogStatus.FAILURE, getString("noshortcuts"));
                }
                return true;

            case 5:
                setState(STATE_SET_ROLE_REQUIREMENTS);
                return true;

            case 6:
                configuration.setRoleRemovals(!configuration.getRoleRemovals());
                setLog(LogStatus.SUCCESS, getString("roleremoveset"));
                return true;

            case 7:
                configuration.setMultipleSlots(!configuration.getMultipleSlots());
                setLog(LogStatus.SUCCESS, getString("multiplerolesset"));
                return true;

            case 8:
                configuration.setSlotOverview(!configuration.getSlotOverview());
                setLog(LogStatus.SUCCESS, getString("roleconnectionsset"));
                return true;

            case 9:
                setState(STATE_UPDATE_COMPONENT_TYPE);
                return true;

            case 10:
                configuration.setRoleCounters(!configuration.getRoleCounters());
                setLog(LogStatus.SUCCESS, getString("rolenumbersset"));
                return true;

            case 11:
                String error = ReactionRoles.checkForErrors(getLocale(), getGuildEntity(), configuration, editMode);
                if (error != null) {
                    setLog(LogStatus.FAILURE, error);
                    return true;
                }

                setState(STATE_EXAMPLE);
                return true;

            case 12:
                error = ReactionRoles.checkForErrors(getLocale(), getGuildEntity(), configuration, editMode);
                if (error != null) {
                    setLog(LogStatus.FAILURE, error);
                    return true;
                }

                getEntityManager().getTransaction().begin();
                if (editMode) {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.REACTION_ROLES_EDIT, event.getMember(), previousTitle);
                } else {
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.REACTION_ROLES_ADD, event.getMember(), configuration.getTitle());
                }
                ReactionRoles.sendMessage(getLocale(), configuration, editMode, getGuildEntity());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("sent"));
                resetRoleMessage(event.getGuild().getIdLong());
                imageCdn = null;
                setState(editMode ? STATE_EDIT : DEFAULT_STATE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = STATE_ADD_SLOT)
    public boolean onButtonAddSlot(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(STATE_CONFIG);
                return true;
            }
            case 0 -> {
                setState(STATE_ADD_SLOT_SET_EMOJI);
                return true;
            }
            case 1 -> {
                setState(STATE_ADD_SLOT_SET_ROLES);
                return true;
            }
            case 2 -> {
                Modal modal = new StringModalBuilder(this, getString("addslot_customlabel"), TextInputStyle.SHORT)
                        .setMinMaxLength(0, CUSTOM_LABEL_MAX_LENGTH)
                        .setGetter(() -> slotConfiguration.getCustomLabel())
                        .setSetter(s -> slotConfiguration.setCustomLabel(s))
                        .build();
                event.replyModal(modal).queue();
                return false;
            }
            case 3 -> {
                if (slotConfiguration.getRoleIds().isEmpty()) {
                    setLog(LogStatus.FAILURE, getString("addslot_noroles"));
                    return true;
                }
                if (slotConfiguration.getRoleIds().size() > 1 && slotConfiguration.getCustomLabel() == null) {
                    setLog(LogStatus.FAILURE, getString("addslot_nocustomlabel"));
                    return true;
                }

                configuration.getSlots().add(slotConfiguration);
                setState(STATE_CONFIG);
                setLog(LogStatus.SUCCESS, getString("linkadded"));
                return true;
            }
        }

        return false;
    }

    @ControllerButton(state = STATE_UPDATE_COMPONENT_TYPE)
    public boolean onButtonUpdateComponentType(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        } else {
            configuration.setComponentType(ReactionRoleEntity.ComponentType.values()[i]);
            setLog(LogStatus.SUCCESS, getString("newcomponentsset"));
            setState(STATE_CONFIG);
            return true;
        }
    }

    @ControllerButton(state = STATE_EXAMPLE)
    public boolean onButtonExample(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        }
        return false;
    }

    @Override
    public boolean onReaction(@NotNull GenericMessageReactionEvent event) {
        return emojiStateProcessor.handleReactionEvent(event);
    }

    @ControllerButton(state = STATE_REMOVE_SLOTS)
    public boolean onButtonRemoveSlot(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        }
        if (i < configuration.getSlots().size() && i != -2) {
            configuration.getSlots().remove(i);
            setLog(LogStatus.SUCCESS, getString("linkremoved"));
            if (configuration.getSlots().isEmpty()) {
                setState(STATE_CONFIG);
            }
            return true;
        }
        return false;
    }

    @ControllerButton
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(STATE_CONFIG);
            return true;
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_SET_CHANNEL)
    public boolean onSelectMenuAddMessage(EntitySelectInteractionEvent event) {
        GuildMessageChannel channel = (GuildMessageChannel) event.getMentions().getChannels().get(0);
        if (checkWriteEmbedInChannelWithLog(channel)) {
            configuration.setMessageChannelId(channel.getIdLong());
            setState(STATE_CONFIG);
        }
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawAddOrEdit(Member member) {
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description", StringUtil.numToString(MAX_ROLE_MESSAGES_FREE)));
    }

    @Draw(state = STATE_SET_CHANNEL)
    public EmbedBuilder onDrawAddMessage(Member member) {
        EntitySelectMenu channelSelectMenu = EntitySelectMenu.create("select_channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                .setRequiredRange(1, 1)
                .build();
        setComponents(channelSelectMenu);
        return EmbedFactory.getEmbedDefault(this, getString("state1_description"), getString("state1_title"));
    }

    @Draw(state = STATE_EDIT)
    public EmbedBuilder onDrawEditMessage(Member member) {
        List<ReactionRoleEntity> reactionRoles = new ArrayList<>(getGuildEntity().getReactionRoles().values());
        String[] options = new String[reactionRoles.size()];
        for (int i = 0; i < reactionRoles.size(); i++) {
            ReactionRoleEntity reactionRoleEntity = reactionRoles.get(i);
            options[i] = getString("state2_template", reactionRoleEntity.getTitle(), reactionRoleEntity.getMessageChannel().getPrefixedName(getLocale()));
        }

        setComponents(options);
        return EmbedFactory.getEmbedDefault(this, getString("state2_description"), getString("state2_title"));
    }

    @Draw(state = STATE_CONFIG)
    public EmbedBuilder onDrawConfigureMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        String[] options = getString("state3_options").split("\n");
        if (configuration.getComponentType() == ReactionRoleEntity.ComponentType.REACTIONS) {
            options[10] = "";
        }
        setComponents(
                options,
                new int[]{12},
                new int[0]
        );

        GuildMessageChannel currentChannel = getGuildMessageChannel().get();
        String linkString = ReactionRoles.generateSlotOverview(configuration.getSlots());
        return EmbedFactory.getEmbedDefault(this, null, getString("state3_title_" + (editMode ? "edit" : "new")))
                .addField(getString("state3_mtitle"), StringUtil.escapeMarkdown(configuration.getTitle()), true)
                .addField(getString("state3_mdescription"), StringUtil.shortenString(StringUtil.escapeMarkdown(Optional.ofNullable(configuration.getDescription()).orElse(notSet)), SLOTS_TEXT_LENGTH_MAX), true)
                .addField(getString("state3_mimage"), StringUtil.getOnOffForBoolean(currentChannel, getLocale(), configuration.getImageFilename() != null), true)
                .addField(getString("state3_mshortcuts"), StringUtil.shortenString(Optional.ofNullable(linkString).orElse(notSet), SLOTS_TEXT_LENGTH_MAX), true)
                .addField(getString("state3_mrolerequirements"), new ListGen<AtomicRole>().getList(configuration.getRoleRequirements(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("state3_mproperties"), getString(
                                configuration.getComponentType() == ReactionRoleEntity.ComponentType.REACTIONS ? "state3_mproperties_desc" : "state3_mproperties_desc_newcomponents",
                                StringUtil.getOnOffForBoolean(currentChannel, getLocale(), configuration.getRoleRemovals()),
                                StringUtil.getOnOffForBoolean(currentChannel, getLocale(), configuration.getMultipleSlots()),
                                StringUtil.getOnOffForBoolean(currentChannel, getLocale(), configuration.getSlotOverview()),
                                getString("componenttypes", configuration.getComponentType().ordinal()),
                                StringUtil.getOnOffForBoolean(currentChannel, getLocale(), configuration.getRoleCounters())
                        ), false
                );
    }

    @Draw(state = STATE_ADD_SLOT)
    public EmbedBuilder onDrawAddSlot(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        String[] options = getString("addslot_options").split("\n");
        setComponents(options, new int[]{3}, new int[0]);

        List<AtomicRole> atomicRoles = new ListAdapter<>(slotConfiguration.getRoleIds(), roleId -> new AtomicRole(member.getGuild().getIdLong(), roleId), AtomicRole::getIdLong);
        return EmbedFactory.getEmbedDefault(this)
                .setTitle(getString("addslot_title"))
                .addField(getString("addslot_emoji"), Objects.requireNonNullElse(slotConfiguration.getEmojiFormatted(), notSet), true)
                .addField(getString("addslot_roles"), new ListGen<AtomicRole>().getList(atomicRoles, getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("addslot_customlabel"), Objects.requireNonNullElse(slotConfiguration.getCustomLabel(), notSet), true);
    }

    @Draw(state = STATE_REMOVE_SLOTS)
    public EmbedBuilder onDrawRemoveSlot(Member member) {
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < configuration.getSlots().size(); i++) {
            ReactionRoleSlotEntity slot = configuration.getSlots().get(i);
            String roleName = new AtomicRole(getGuildId().get(), slot.getRoleIds().get(0)).getPrefixedName(getLocale());
            String label = slot.getCustomLabel() != null
                    ? slot.getCustomLabel()
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

    @Draw(state = STATE_UPDATE_COMPONENT_TYPE)
    public EmbedBuilder onDrawUpdateComponentType(Member member) {
        String[] options = new String[3];
        for (int i = 0; i < ReactionRoleEntity.ComponentType.values().length; i++) {
            options[i] = getString("componenttypes", i);
        }
        setComponents(options);

        return EmbedFactory.getEmbedDefault(this, getString("state11_description"), getString("state11_title"));
    }

    @Draw(state = STATE_EXAMPLE)
    public EmbedBuilder onDrawExample(Member member) throws ExecutionException, InterruptedException {
        setActionRows(ReactionRoles.getComponents(getLocale(), member.getGuild(), configuration));
        return ReactionRoles.getMessageEmbed(getLocale(), configuration);
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        ReactionRoleEntity reactionRole = getGuildEntity().getReactionRoles().get(event.getMessageIdLong());
        if (reactionRole == null) {
            return;
        }
        List<ReactionRoleSlotEntity> slots = reactionRole.getSlots();

        ReactionRoleSlotEntity currentSlot = null;
        for (ReactionRoleSlotEntity slot : slots) {
            if (EmojiUtil.equals(event.getEmoji(), slot.getEmoji())) {
                currentSlot = slot;
                break;
            }
        }
        if (currentSlot == null) {
            return;
        }

        MessageEmbed responseEmbed = processRoleMessageInteraction(event.getMember(), reactionRole, null, false, List.of(currentSlot), Collections.emptyList());
        if (responseEmbed != null && USER_DM_CACHE.getIfPresent(event.getUserIdLong()) == null) {
            if (Program.productionMode()) {
                USER_DM_CACHE.put(event.getUserIdLong(), true);
            }
            JDAUtil.openPrivateChannel(event.getUser())
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(responseEmbed))
                    .queue();
        }
    }

    @Override
    public void onStaticReactionRemove(@NotNull Message message, @NotNull MessageReactionRemoveEvent event) {
        ReactionRoleEntity reactionRole = getGuildEntity().getReactionRoles().get(event.getMessageIdLong());
        if (reactionRole == null) {
            return;
        }
        List<ReactionRoleSlotEntity> slots = reactionRole.getSlots();

        ReactionRoleSlotEntity currentSlot = null;
        for (ReactionRoleSlotEntity slot : slots) {
            if (EmojiUtil.equals(event.getEmoji(), slot.getEmoji())) {
                currentSlot = slot;
                break;
            }
        }
        if (currentSlot == null) {
            return;
        }

        MessageEmbed responseEmbed = processRoleMessageInteraction(event.getMember(), reactionRole, null, false, Collections.emptyList(), List.of(currentSlot));
        if (responseEmbed != null && USER_DM_CACHE.getIfPresent(event.getUserIdLong()) == null) {
            if (Program.productionMode()) {
                USER_DM_CACHE.put(event.getUserIdLong(), true);
            }
            JDAUtil.openPrivateChannel(event.getUser())
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(responseEmbed))
                    .queue();
        }
    }

    @Override
    public void onStaticButton(@NotNull ButtonInteractionEvent event, @Nullable String secondaryId) {
        ReactionRoleEntity reactionRole = getGuildEntity().getReactionRoles().get(event.getMessageIdLong());
        if (reactionRole == null) {
            return;
        }
        List<ReactionRoleSlotEntity> slots = reactionRole.getSlots();

        int slotId = Integer.parseInt(event.getComponentId());
        if (slotId < 0 || slotId >= slots.size()) {
            return;
        }
        ReactionRoleSlotEntity slot = slots.get(slotId);

        HashSet<ReactionRoleSlotEntity> addSlots = new HashSet<>();
        HashSet<ReactionRoleSlotEntity> removeSlots = new HashSet<>();

        if (new HashSet<>(event.getMember().getRoles()).containsAll(slot.getRoles(event.getGuild()))) {
            removeSlots.add(slot);
        } else {
            addSlots.add(slot);
        }

        MessageEmbed responseEmbed = processRoleMessageInteraction(event.getMember(), reactionRole, event, true, addSlots, removeSlots);
        if (responseEmbed != null) {
            event.getHook()
                    .sendMessageEmbeds(responseEmbed)
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void onStaticStringSelectMenu(@NotNull StringSelectInteractionEvent event, @Nullable String secondaryId) {
        ReactionRoleEntity reactionRole = getGuildEntity().getReactionRoles().get(event.getMessageIdLong());
        if (reactionRole == null) {
            return;
        }
        List<ReactionRoleSlotEntity> slots = reactionRole.getSlots();

        HashSet<ReactionRoleSlotEntity> addSlots = new HashSet<>();
        HashSet<ReactionRoleSlotEntity> removeSlots = new HashSet<>(slots);
        for (String value : event.getValues()) {
            int slotId = Integer.parseInt(value);
            if (slotId >= 0 && slotId < slots.size()) {
                addSlots.add(slots.get(slotId));
                removeSlots.remove(slots.get(slotId));
            }
        }

        MessageEmbed responseEmbed = processRoleMessageInteraction(event.getMember(), reactionRole, event, true, addSlots, removeSlots);
        if (responseEmbed != null) {
            event.getHook()
                    .sendMessageEmbeds(responseEmbed)
                    .setEphemeral(true)
                    .queue();
        }
    }

    private void resetRoleMessage(long guildId) {
        configuration = new ReactionRoleEntity();
        configuration.setMessageGuildId(guildId);
        configuration.setTitle(Command.getCommandLanguage(ReactionRolesCommand.class, getLocale()).getTitle());
    }

    private void deleteTemporaryImage() {
        if (imageCdn != null) {
            imageCdn.delete();
            imageCdn = null;
        }
    }

    private MessageEmbed processRoleMessageInteraction(Member member, ReactionRoleEntity reactionRole, GenericComponentInteractionCreateEvent componentEvent,
                                                       boolean returnEmbedOnSuccess, Collection<ReactionRoleSlotEntity> addSlots, Collection<ReactionRoleSlotEntity> removeSlots
    ) {
        if (reactionRole == null || BLOCK_CACHE.asMap().containsKey(member.getIdLong())) {
            return null;
        }
        if (componentEvent != null) {
            componentEvent.deferEdit().queue();
        }
        if (!ServerPatreonBoostCache.get(member.getGuild().getIdLong()) &&
                getGuildEntity().getReactionRoles().size() > MAX_ROLE_MESSAGES_FREE &&
                reactionRole.getNewGeneration()
        ) {
            return EmbedFactory.getEmbedError(this, getString("limitexceeded")).build();
        }
        if (violatesRoleRequirements(reactionRole, member)) {
            return EmbedFactory.getEmbedError(this, getString("components_result_rolerequirements")).build();
        }

        if (ServerPatreonBoostCache.get(member.getGuild().getIdLong()) &&
                getGuildEntity().getReactionRoles().size() > MAX_ROLE_MESSAGES_FREE
        ) {
            FeatureLogger.inc(PremiumFeature.REACTION_ROLES_LIMIT, member.getGuild().getIdLong());
        }

        try {
            BLOCK_CACHE.put(member.getIdLong(), true);

            HashSet<Role> checkedRoles = new HashSet<>();
            HashSet<Role> addedRoles = new HashSet<>();
            HashSet<Role> unmanageableRoles = new HashSet<>();
            HashSet<Role> removedRoles = new HashSet<>();

            for (ReactionRoleSlotEntity slot : addSlots) {
                for (Long roleId : slot.getRoleIds()) {
                    Role role = member.getGuild().getRoleById(roleId);
                    if (role == null) {
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

            HashSet<ReactionRoleSlotEntity> removeSlotsSet = new HashSet<>(removeSlots);
            if (!addedRoles.isEmpty() && !reactionRole.getMultipleSlots()) {
                removeSlotsSet.addAll(reactionRole.getSlots());
            }

            for (ReactionRoleSlotEntity slot : removeSlotsSet) {
                for (Long roleId : slot.getRoleIds()) {
                    Role role = member.getGuild().getRoleById(roleId);
                    if (role == null || !member.getRoles().contains(role) || checkedRoles.contains(role)) {
                        continue;
                    }

                    if (PermissionCheckRuntime.botCanManageRoles(getLocale(), getClass(), role)) {
                        removedRoles.add(role);
                    } else {
                        unmanageableRoles.add(role);
                    }
                }
            }

            if (!removedRoles.isEmpty() && !reactionRole.getRoleRemovals()) {
                if (reactionRole.getRoleCounters() && componentEvent != null) {
                    componentEvent.getHook().editOriginalComponents(ReactionRoles.getComponents(getLocale(), member.getGuild(), reactionRole)).queue();
                }
                String textKey = !reactionRole.getMultipleSlots() && !addSlots.isEmpty() ? "components_result_onlyone" : "components_result_noremoval";
                return EmbedFactory.getEmbedError(this, getString(textKey)).build();
            }

            ArrayList<Role> memberRoles = new ArrayList<>(member.getRoles());
            memberRoles.removeAll(removedRoles);
            memberRoles.addAll(addedRoles);
            if (!addedRoles.isEmpty() || !removedRoles.isEmpty()) {
                member.getGuild().modifyMemberRoles(member, memberRoles)
                        .reason(getCommandLanguage().getTitle())
                        .complete();
            }

            if (reactionRole.getRoleCounters() && componentEvent != null) {
                Thread.sleep(100);
                componentEvent.getHook().editOriginalComponents(ReactionRoles.getComponents(getLocale(), member.getGuild(), reactionRole)).queue();
            }
            return returnEmbedOnSuccess ? generateRoleSummary(addedRoles, removedRoles, unmanageableRoles).build() : null;
        } catch (ExecutionException | InterruptedException e) {
            MainLogger.get().error("Error in reaction roles component update", e);
        } finally {
            BLOCK_CACHE.invalidate(member.getIdLong());
        }
        return null;
    }

    private String uploadFile(LocalFile file) {
        deleteTemporaryImage();
        imageCdn = file;
        return file.cdnGetUrl();
    }

    private EmbedBuilder generateRoleSummary(Collection<Role> addedRoles, Collection<Role> removedRoles, Collection<Role> unmanageableRoles) {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this);
        if (addedRoles.isEmpty() && removedRoles.isEmpty()) {
            eb.setDescription(getString("components_result_noupdates"));
        } else {
            if (!removedRoles.isEmpty()) {
                eb.addField(getString("components_result_removedroles"), new ListGen<Role>().getList(removedRoles, ListGen.SLOT_TYPE_BULLET, r -> new AtomicRole(r).getPrefixedNameInField(getLocale())), true);
            }
            if (!addedRoles.isEmpty()) {
                eb.addField(getString("components_result_newroles"), new ListGen<Role>().getList(addedRoles, ListGen.SLOT_TYPE_BULLET, r -> new AtomicRole(r).getPrefixedNameInField(getLocale())), true);
            }
        }

        if (!unmanageableRoles.isEmpty()) {
            EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_role", unmanageableRoles.size() != 1, MentionUtil.getMentionedStringOfRoles(getLocale(), unmanageableRoles).getMentionText().replace("**", "\"")));
        }

        return eb;
    }

    private boolean violatesRoleRequirements(ReactionRoleEntity reactionRoleEntity, Member member) {
        return !reactionRoleEntity.getRoleRequirements().isEmpty() &&
                reactionRoleEntity.getRoleRequirements().stream().noneMatch(atomicRole -> member.getRoles().stream().anyMatch(r -> r.getIdLong() == atomicRole.getIdLong()));
    }

}
