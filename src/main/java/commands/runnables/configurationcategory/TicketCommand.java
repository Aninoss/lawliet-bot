package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnStaticButtonListener;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticStringSelectMenuListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.*;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.LocalFile;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicStandardGuildMessageChannel;
import core.cache.ServerPatreonBoostCache;
import core.interactionresponse.ComponentInteractionResponse;
import core.modals.DurationModalBuilder;
import core.modals.ModalMediator;
import core.utils.*;
import kotlin.Pair;
import modules.Ticket;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import mysql.hibernate.entity.guild.TicketsEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.modals.Modal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "ticket",
        botChannelPermissions = {Permission.MESSAGE_EXT_EMOJI},
        botGuildPermissions = {Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES},
        userGuildPermissions = {Permission.MANAGE_CHANNEL},
        releaseDate = {2021, 5, 24},
        emoji = "🎟️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"tickets", "supportticket", "supporttickets"}
)
public class TicketCommand extends NavigationAbstract implements OnStaticReactionAddListener, OnStaticButtonListener, OnStaticStringSelectMenuListener {

    public final static int MAX_STAFF_ROLES = 10;
    public final static int MAX_GREETING_TEXT_LENGTH = 1000;
    public final static int MAX_CREATE_MESSAGE_CONTENT_LENGTH = 1000;
    public final static int MAX_CREATE_MESSAGE_CATEGORIES = StringSelectMenu.OPTIONS_MAX_AMOUNT;
    public final static int MAX_CREATE_MESSAGE_CATEGORY_LENGTH = 50;

    public final static UnicodeEmoji TICKET_CLOSE_EMOJI = Emojis.X;
    public final static String COMPONENT_ID_CREATE = "create";
    public final static String BUTTON_ID_CLOSE = "close";
    public final static String BUTTON_ID_ASSIGN = "assign";

    private final static int
            STATE_SET_LOG_CHANNEL = 1,
            STATE_SET_STAFF_ROLES = 2,
            STATE_CREATE_TICKET_MESSAGE = 4,
            STATE_SET_CREATE_MESSATE_CHANNEL = 8,
            STATE_SET_CREATE_MESSAGE_CONTENT = 9,
            STATE_SET_CREATE_MESSAGE_CATEGORIES = 11,
            STATE_SET_CREATE_MESSAGE_IMAGE = 10,
            STATE_SET_GREETING_TEXT = 5,
            STATE_SET_ASSIGNMENT_MODE = 6,
            STATE_SET_CLOSE_ON_INACTIVITY = 7;

    private AtomicStandardGuildMessageChannel createMessageAtomicChannel;
    private String createMessageContent;
    private List<String> createMessageCategories = Collections.emptyList();
    private LocalFile createMessageFile = null;
    private boolean createMessageContentChanged = false;

    public TicketCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        registerNavigationListener(event.getMember(), List.of(
                new GuildChannelsStateProcessor(this, STATE_SET_LOG_CHANNEL, DEFAULT_STATE, getString("state0_mannouncement"))
                        .setDescription(getString("state1_description"))
                        .setMinMax(0, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setLogEvent(BotLogEntity.Event.TICKETS_LOG_CHANNEL)
                        .setSingleGetter(() -> getGuildEntity().getTickets().getLogChannelId())
                        .setSingleSetter(channelId -> getGuildEntity().getTickets().setLogChannelId(channelId)),
                new RolesStateProcessor(this, STATE_SET_STAFF_ROLES, DEFAULT_STATE, getString("state0_mstaffroles"))
                        .setDescription(getString("state2_description"))
                        .setMinMax(0, MAX_STAFF_ROLES)
                        .setCheckAccess(false)
                        .setLogEvent(BotLogEntity.Event.TICKETS_STAFF_ROLES)
                        .setGetter(() -> getGuildEntity().getTickets().getStaffRoleIds())
                        .setSetter(roleIds -> CollectionUtil.replace(getGuildEntity().getTickets().getStaffRoleIds(), roleIds)),
                new StringStateProcessor(this, STATE_SET_GREETING_TEXT, DEFAULT_STATE, getString("state0_mcreatemessage"))
                        .setClearButton(true)
                        .setMax(MAX_GREETING_TEXT_LENGTH)
                        .setLogEvent(BotLogEntity.Event.TICKETS_GREETING_TEXT)
                        .setGetter(() -> getGuildEntity().getTickets().getGreetingText())
                        .setSetter(input -> getGuildEntity().getTickets().setGreetingText(input)),
                new GuildChannelsStateProcessor(this, STATE_SET_CREATE_MESSATE_CHANNEL, STATE_CREATE_TICKET_MESSAGE, getString("state4_mchannel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.STANDARD_GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)
                        .setSingleGetter(() -> createMessageAtomicChannel != null ? createMessageAtomicChannel.getIdLong() : null)
                        .setSingleSetter(channelId -> createMessageAtomicChannel = new AtomicStandardGuildMessageChannel(getGuildId().get(), channelId)),
                new StringStateProcessor(this, STATE_SET_CREATE_MESSAGE_CONTENT, STATE_CREATE_TICKET_MESSAGE, getString("state4_mtext"))
                        .setClearButton(false)
                        .setMax(MAX_CREATE_MESSAGE_CONTENT_LENGTH)
                        .setGetter(() -> createMessageContent)
                        .setSetter(input -> {
                            createMessageContent = input;
                            createMessageContentChanged = true;
                        }),
                new StringListStateProcessor(this, STATE_SET_CREATE_MESSAGE_CATEGORIES, STATE_CREATE_TICKET_MESSAGE, getString("state4_mcategories"))
                        .setMax(MAX_CREATE_MESSAGE_CATEGORIES, MAX_CREATE_MESSAGE_CATEGORY_LENGTH)
                        .setStringSplitterFunction(str -> List.of(str.split("\n")))
                        .setGetter(() -> createMessageCategories)
                        .setSetter(categories -> createMessageCategories = categories),
                new FileStateProcessor(this, STATE_SET_CREATE_MESSAGE_IMAGE, STATE_CREATE_TICKET_MESSAGE, getString("state4_mimage"))
                        .setAllowGifs(true)
                        .setClearButton(true)
                        .setGetter(() -> createMessageFile != null ? createMessageFile.getName() : null)
                        .setSetter(attachment -> {
                            if (attachment != null) {
                                LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("temp/%s.%s", RandomUtil.generateRandomString(30), attachment.getFileExtension()));
                                FileUtil.downloadImageAttachment(attachment, tempFile);
                                createMessageFile = tempFile;
                                createMessageContentChanged = true;
                            } else {
                                createMessageFile = null;
                            }
                        })
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        TicketsEntity tickets = getGuildEntity().getTickets();

        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                setState(STATE_SET_LOG_CHANNEL);
                return true;
            }
            case 1 -> {
                setState(STATE_SET_STAFF_ROLES);
                return true;
            }
            case 2 -> {
                setState(STATE_SET_ASSIGNMENT_MODE);
                return true;
            }
            case 3 -> {
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setState(STATE_SET_CLOSE_ON_INACTIVITY);
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                }
                return true;
            }
            case 4 -> {
                setState(STATE_SET_GREETING_TEXT);
                return true;
            }
            case 5 -> {
                tickets.beginTransaction();
                tickets.setPingStaffRoles(!tickets.getPingStaffRoles());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_PING_STAFF_ROLES, event.getMember(), null, tickets.getPingStaffRoles());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getPingStaffRoles(), getString("state0_mping")));
                return true;
            }
            case 6 -> {
                tickets.beginTransaction();
                tickets.setEnforceModal(!tickets.getEnforceModal());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_ENFORCE_MODAL, event.getMember(), null, tickets.getEnforceModal());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getEnforceModal(), getString("state0_mtextinput")));
                return true;
            }
            case 7 -> {
                tickets.beginTransaction();
                tickets.setMembersCanCloseTickets(!tickets.getMembersCanCloseTickets());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_MEMBERS_CAN_CLOSE_TICKETS, event.getMember(), null, tickets.getMembersCanCloseTickets());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getMembersCanCloseTickets(), getString("state0_mmembercanclose")));
                return true;
            }
            case 8 -> {
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    tickets.beginTransaction();
                    tickets.setProtocols(!tickets.getProtocols());
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_PROTOCOLS, event.getMember(), null, tickets.getProtocolsEffectively());
                    tickets.commitTransaction();
                    setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getProtocolsEffectively(), getString("state0_mprotocol")));
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                }
                return true;
            }
            case 9 -> {
                tickets.beginTransaction();
                tickets.setDeleteChannelsOnClose(!tickets.getDeleteChannelsOnClose());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_DELETE_CHANNELS_ON_CLOSE, event.getMember(), null, tickets.getDeleteChannelsOnClose());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getDeleteChannelsOnClose(), getString("state0_mdeletechannel")));
                return true;
            }
            case 10 -> {
                createMessageAtomicChannel = new AtomicStandardGuildMessageChannel(event.getGuild().getIdLong(), 0L);
                createMessageContent = getString("message_content");
                createMessageCategories = Collections.emptyList();
                createMessageFile = null;
                createMessageContentChanged = false;
                setState(STATE_CREATE_TICKET_MESSAGE);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = STATE_SET_ASSIGNMENT_MODE)
    public boolean onButtonAssignmentMode(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }

        TicketsEntity tickets = getGuildEntity().getTickets();
        tickets.beginTransaction();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_ASSIGNMENT_MODE, event.getMember(), tickets.getAssignmentMode(), TicketsEntity.AssignmentMode.values()[i]);
        tickets.setAssignmentMode(TicketsEntity.AssignmentMode.values()[i]);
        tickets.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("assignment_set", getString("assignment_modes").split("\n")[i]));
        setState(DEFAULT_STATE);
        return true;
    }

    @ControllerButton(state = STATE_SET_CLOSE_ON_INACTIVITY)
    public boolean onButtonCloseOnInactivity(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                Modal modal = new DurationModalBuilder(this, getString("state7_mticketage"))
                        .setMinMinutes(60)
                        .enableHibernateTransaction()
                        .setGetterInt(() -> {
                            Integer autoCloseMinutes = getGuildEntity().getTickets().getAutoCloseHoursEffectively();
                            if (autoCloseMinutes != null) {
                                autoCloseMinutes *= 60;
                            }
                            return autoCloseMinutes;
                        })
                        .setSetter(minutes -> {
                            TicketsEntity tickets = getGuildEntity().getTickets();
                            Integer autoCloseMinutes = tickets.getAutoCloseHoursEffectively();
                            if (autoCloseMinutes != null) {
                                autoCloseMinutes *= 60;
                            }

                            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_AUTO_CLOSE, event.getMember(), autoCloseMinutes, minutes);
                            tickets.setAutoCloseHours((int) (minutes / 60));
                            setState(DEFAULT_STATE);
                        })
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 1 -> {
                TicketsEntity tickets = getGuildEntity().getTickets();
                Integer autoCloseMinutes = tickets.getAutoCloseHoursEffectively();
                if (autoCloseMinutes != null) {
                    autoCloseMinutes *= 60;
                }

                tickets.beginTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_AUTO_CLOSE, event.getMember(), autoCloseMinutes, null);
                tickets.setAutoCloseHours(null);
                tickets.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("autoclose_set"));
                setState(DEFAULT_STATE);
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = STATE_CREATE_TICKET_MESSAGE)
    public boolean onButtonCreateTicketMessage(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(DEFAULT_STATE);
                return true;
            }
            case 0 -> {
                setState(STATE_SET_CREATE_MESSATE_CHANNEL);
                return true;
            }
            case 1 -> {
                setState(STATE_SET_CREATE_MESSAGE_CONTENT);
                return true;
            }
            case 2 -> {
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setState(STATE_SET_CREATE_MESSAGE_CATEGORIES);
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                }
                return true;
            }
            case 3 -> {
                setState(STATE_SET_CREATE_MESSAGE_IMAGE);
                return true;
            }
            case 4 -> {
                StandardGuildMessageChannel channel = createMessageAtomicChannel.get().orElse(null);
                if (channel == null) {
                    createMessageAtomicChannel = null;
                    return true;
                }

                String error = Ticket.sendTicketMessage(getGuildEntity(), getLocale(), channel, createMessageContent, createMessageCategories, createMessageFile, createMessageContentChanged);
                if (error != null) {
                    setLog(LogStatus.FAILURE, error);
                    return true;
                }

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_CREATE_TICKET_MESSAGE, event.getMember(), channel.getId());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("message_sent"));
                setState(DEFAULT_STATE);
                return true;
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) {
        TicketsEntity tickets = getGuildEntity().getTickets();
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this)
                .addField(getString("state0_mannouncement"), tickets.getLogChannelId() != null ? tickets.getLogChannel().getPrefixedNameInField(getLocale()) : notSet, true)
                .addField(getString("state0_mstaffroles"), new ListGen<AtomicRole>().getList(tickets.getStaffRoles(), getLocale(), m -> m.getPrefixedNameInField(getLocale())), true)
                .addField(getString("state0_massign"), getString("assignment_modes").split("\n")[tickets.getAssignmentMode().ordinal()], true)
                .addField(getString("state0_mcloseoninactivity") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), getCloseOnInactivityValue(tickets), true)
                .addField(getString("state0_mcreatemessage"), StringUtil.shortenString(tickets.getGreetingText() != null ? StringUtil.escapeMarkdown(tickets.getGreetingText()) : notSet, 1024), false)
                .addField(getString("state0_mcreateoptions"), generateCreateOptionsField(tickets), false)
                .addField(getString("state0_mcloseoptions"), generateCloseOptionsField(tickets), false);
    }

    @Draw(state = STATE_SET_ASSIGNMENT_MODE)
    public EmbedBuilder onDrawAssignmentMode(Member member) {
        setComponents(getString("assignment_modes").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state6_description"),
                getString("state6_title")
        );
    }

    @Draw(state = STATE_SET_CLOSE_ON_INACTIVITY)
    public EmbedBuilder onDrawCloseOnInactivity(Member member) {
        setComponents(getString("state7_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state7_description"),
                getString("state7_title")
        );
    }

    @Draw(state = STATE_CREATE_TICKET_MESSAGE)
    public EmbedBuilder onDrawCreateTicketMessage(Member member) {
        String[] options = getString("state4_options").split("\n");
        setComponents(options, Set.of(4), null, createMessageAtomicChannel.get().isEmpty() ? Set.of(4) : null);

        return EmbedFactory.getEmbedDefault(this, getString("state4_description"), getString("state4_title"))
                .addField(getString("state4_mchannel"), createMessageAtomicChannel.getPrefixedNameInField(getLocale()), true)
                .addField(getString("state4_mtext"), createMessageContent, true)
                .addField(getString("state4_mcategories") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), new ListGen<String>().getList(createMessageCategories, getString("state4_notconfigured"), str -> str), true)
                .addField(getString("state4_mimage"), StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), createMessageFile != null), true);
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        if (!(event.getChannel() instanceof StandardGuildMessageChannel)) {
            return;
        }
        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();

        TicketsEntity ticketsEntity = getGuildEntity().getTickets();
        TicketChannelEntity ticketChannelEntity = ticketsEntity.getTicketChannels().get(event.getChannel().getIdLong());

        if (ticketChannelEntity == null && event.getEmoji().getFormatted().equals(getCommandProperties().emoji())) {
            Category category = channel.getParentCategory();
            if (category == null || category.getChannels().size() < 50) {
                Ticket.createTicket(getGuildEntity(), channel, event.getMember(), null, null);
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                JDAUtil.openPrivateChannel(event.getMember())
                        .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                        .queue();
            }
        } else if (ticketChannelEntity != null && EmojiUtil.equals(event.getEmoji(), TICKET_CLOSE_EMOJI)) {
            if (memberIsStaff(event.getMember(), ticketsEntity.getStaffRoleIds()) ||
                    (event.getMember().getIdLong() == ticketChannelEntity.getMemberId() && ticketsEntity.getMembersCanCloseTickets())
            ) {
                if (ticketChannelEntity.getIntroductionMessageId() == 0L) {
                    ticketsEntity.beginTransaction();
                    ticketChannelEntity.setIntroductionMessageId(event.getMessageIdLong());
                    ticketsEntity.commitTransaction();
                }

                Ticket.closeTicket(getGuildEntity(), ticketChannelEntity, channel, event.getMember());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                channel.sendMessageEmbeds(eb.build())
                        .queue();
            }
        }
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event, String secondaryId) throws ExecutionException, InterruptedException {
        GuildChannel channelTemp = secondaryId == null
                ? event.getGuildChannel()
                : event.getGuild().getGuildChannelById(secondaryId);

        if (channelTemp == null) {
            EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("channeldelete"));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        if (!(channelTemp instanceof StandardGuildMessageChannel)) {
            return;
        }

        TicketsEntity ticketsEntity = getGuildEntity().getTickets();
        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) channelTemp;
        TicketChannelEntity ticketChannelEntity = ticketsEntity.getTicketChannels().get(channel.getIdLong());

        if (ticketChannelEntity == null && event.getComponentId().equals(COMPONENT_ID_CREATE)) {
            createTicket(event, ticketsEntity, channel, null);
        } else if (ticketChannelEntity != null) {
            if (event.getComponentId().equals(BUTTON_ID_CLOSE)) {
                if (memberIsStaff(event.getMember(), ticketsEntity.getStaffRoleIds()) ||
                        (event.getMember().getIdLong() == ticketChannelEntity.getMemberId() && ticketsEntity.getMembersCanCloseTickets())
                ) {
                    if (secondaryId == null && ticketChannelEntity.getIntroductionMessageId() == 0L) {
                        ticketsEntity.beginTransaction();
                        ticketChannelEntity.setIntroductionMessageId(event.getMessageIdLong());
                        ticketsEntity.commitTransaction();
                    }
                    Ticket.closeTicket(getGuildEntity(), ticketChannelEntity, channel, event.getMember());
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                    event.replyEmbeds(eb.build())
                            .setEphemeral(true)
                            .queue();
                }
            } else if (event.getComponentId().equals(BUTTON_ID_ASSIGN)) {
                if (memberIsStaff(event.getMember(), ticketsEntity.getStaffRoleIds())) {
                    Ticket.assignTicket(event.getMember(), channel, getGuildEntity(), ticketChannelEntity);
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotassign"));
                    event.replyEmbeds(eb.build())
                            .setEphemeral(true)
                            .queue();
                }
            }
        }
        new ComponentInteractionResponse(event).complete();
    }

    @Override
    public void onStaticStringSelectMenu(@NotNull StringSelectInteractionEvent event, @Nullable String secondaryId) {
        GuildChannel channelTemp = event.getGuildChannel();
        if (!(channelTemp instanceof StandardGuildMessageChannel)) {
            return;
        }

        TicketsEntity ticketsEntity = getGuildEntity().getTickets();
        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) channelTemp;

        createTicket(event, ticketsEntity, channel, event.getSelectedOptions().get(0).getLabel());
        new ComponentInteractionResponse(event).complete();
        event.getHook().editOriginal("").queue();
    }

    private void createTicket(GenericComponentInteractionCreateEvent event, TicketsEntity ticketsEntity, StandardGuildMessageChannel channel, String selectedCategory) {
        if (ticketsEntity.getEnforceModal()) {
            TextInput message = TextInput.create("message", TextInputStyle.PARAGRAPH)
                    .setPlaceholder(getString("modal_message_placeholder"))
                    .setMinLength(30)
                    .setMaxLength(1000)
                    .build();

            Modal modal = ModalMediator.createModal(event.getUser().getIdLong(), getString("button_create"), (e, guildEntity) -> extractModal(e, guildEntity, selectedCategory))
                    .addComponents(Label.of(getString("modal_message"), message))
                    .build();

            event.replyModal(modal).queue();
        } else {
            Category category = channel.getParentCategory();
            if (category == null || category.getChannels().size() < 50) {
                Ticket.createTicket(getGuildEntity(), channel, event.getMember(), null, selectedCategory);
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    private void extractModal(ModalInteractionEvent event, GuildEntity guildEntity, String selectedCategory) {
        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getChannel();
        Category category = channel.getParentCategory();

        if (category == null || category.getChannels().size() < 50) {
            Ticket.createTicket(guildEntity, channel, event.getMember(), event.getValue("message").getAsString(), selectedCategory);
            event.deferEdit().queue();
        } else {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "rejected"))
                    .setDescription(TextManager.getString(guildEntity.getLocale(), commands.Category.CONFIGURATION, "ticket_toomanychannels"));
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
        }
    }

    private String getCloseOnInactivityValue(TicketsEntity tickets) {
        Integer autoCloseHours = tickets.getAutoCloseHoursEffectively();
        if (autoCloseHours == null) {
            return StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), false);
        } else {
            return TimeUtil.getDurationString(getLocale(), Duration.ofHours(autoCloseHours));
        }
    }

    private String generateCreateOptionsField(TicketsEntity tickets) {
        return generateOptionsString(
                List.of(
                        new Pair<>(getString("state0_mping"), tickets.getPingStaffRoles()),
                        new Pair<>(getString("state0_mtextinput"), tickets.getEnforceModal())
                )
        );
    }

    private String generateCloseOptionsField(TicketsEntity tickets) {
        return generateOptionsString(
                List.of(
                        new Pair<>(getString("state0_mmembercanclose"), tickets.getMembersCanCloseTickets()),
                        new Pair<>(getString("state0_mprotocol") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), tickets.getProtocolsEffectively()),
                        new Pair<>(getString("state0_mdeletechannel"), tickets.getDeleteChannelsOnClose())
                )
        );
    }

    private String generateOptionsString(List<Pair<String, Boolean>> attributesList) {
        StringBuilder sb = new StringBuilder();
        for (Pair<String, Boolean> attribute : attributesList) {
            sb.append("- ")
                    .append(attribute.getFirst())
                    .append(": ")
                    .append(StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), attribute.getSecond()))
                    .append("\n");
        }
        return sb.toString();
    }

    private boolean memberIsStaff(Member member, List<Long> staffRoleIds) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                staffRoleIds.stream().anyMatch(roleId -> member.getRoles().stream().anyMatch(r -> roleId == r.getIdLong()));
    }

}
