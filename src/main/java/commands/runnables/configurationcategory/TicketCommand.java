package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnStaticButtonListener;
import commands.listeners.OnStaticReactionAddListener;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.RolesStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.cache.ServerPatreonBoostCache;
import core.interactionresponse.ComponentInteractionResponse;
import core.utils.*;
import events.discordevents.modalinteraction.ModalInteractionTicket;
import kotlin.Pair;
import modules.Ticket;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import mysql.hibernate.entity.guild.TicketsEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
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
public class TicketCommand extends NavigationAbstract implements OnStaticReactionAddListener, OnStaticButtonListener {

    public final static int MAX_STAFF_ROLES = 10;
    public final static int MAX_GREETING_TEXT_LENGTH = 1000;

    public final static UnicodeEmoji TICKET_CLOSE_EMOJI = Emojis.X;
    public final static String BUTTON_ID_CREATE = "create";
    public final static String BUTTON_ID_CLOSE = "close";
    public final static String BUTTON_ID_ASSIGN = "assign";

    private final static int
            STATE_SET_LOG_CHANNEL = 1,
            STATE_SET_STAFF_ROLES = 2,
            STATE_CREATE_TICKET_MESSAGE = 4,
            STATE_SET_GREETING_TEXT = 5,
            STATE_SET_ASSIGNMENT_MODE = 6,
            STATE_SET_CLOSE_ON_INACTIVITY = 7;

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
                        .setSetter(roleIds -> getGuildEntity().getTickets().setStaffRoleIds(roleIds)),
                new StringStateProcessor(this, STATE_SET_GREETING_TEXT, DEFAULT_STATE, getString("state0_mcreatemessage"))
                        .setClearButton(true)
                        .setMax(MAX_GREETING_TEXT_LENGTH)
                        .setLogEvent(BotLogEntity.Event.TICKETS_GREETING_TEXT)
                        .setGetter(() -> getGuildEntity().getTickets().getGreetingText())
                        .setSetter(input -> getGuildEntity().getTickets().setGreetingText(input))
        ));
        return true;
    }

    @ControllerMessage(state = STATE_SET_CLOSE_ON_INACTIVITY)
    public MessageInputResponse onMessageCloseOnInactivity(MessageReceivedEvent event, String input) {
        int hours = (int) (MentionUtil.getTimeMinutes(input).getValue() / 60);
        if (hours > 0) {
            TicketsEntity tickets = getGuildEntity().getTickets();
            Integer autoCloseMinutes = tickets.getAutoCloseHoursEffectively();
            if (autoCloseMinutes != null) {
                autoCloseMinutes *= 60;
            }

            tickets.beginTransaction();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_AUTO_CLOSE, event.getMember(), autoCloseMinutes, hours * 60);
            tickets.setAutoCloseHours(hours);
            tickets.commitTransaction();

            setLog(LogStatus.SUCCESS, getString("autoclose_set"));
            setState(DEFAULT_STATE);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
            return MessageInputResponse.FAILED;
        }
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
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        } else if (i == 0) {
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
        return false;
    }

    @ControllerButton(state = STATE_CREATE_TICKET_MESSAGE)
    public boolean onButtonCreateTicketMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        }
        return false;
    }

    @ControllerEntitySelectMenu(state = STATE_CREATE_TICKET_MESSAGE)
    public boolean onSelectMenuCreateTicketMessage(EntitySelectInteractionEvent event) {
        StandardGuildMessageChannel channel = (StandardGuildMessageChannel) event.getMentions().getChannels().get(0);
        String error = Ticket.sendTicketMessage(getGuildEntity(), getLocale(), channel);
        if (error == null) {
            getEntityManager().getTransaction().begin();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_CREATE_TICKET_MESSAGE, event.getMember(), channel.getId());
            getEntityManager().getTransaction().commit();

            setLog(LogStatus.SUCCESS, getString("message_sent"));
            setState(DEFAULT_STATE);
        } else {
            setLog(LogStatus.FAILURE, error);
        }
        return true;
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

    private String getCloseOnInactivityValue(TicketsEntity tickets) {
        Integer autoCloseMinutes = tickets.getAutoCloseHoursEffectively();
        if (autoCloseMinutes == null) {
            return StringUtil.getOnOffForBoolean(getGuildMessageChannel().get(), getLocale(), false);
        } else {
            return TimeUtil.getDurationString(getLocale(), Duration.ofMinutes(autoCloseMinutes));
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
        EntitySelectMenu entitySelectMenu = EntitySelectMenu.create("channel", EntitySelectMenu.SelectTarget.CHANNEL)
                .setChannelTypes(JDAUtil.STANDARD_GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                .setRequiredRange(1, 1)
                .build();
        setComponents(entitySelectMenu);

        return EmbedFactory.getEmbedDefault(
                this,
                getString("state4_description"),
                getString("state4_title")
        );
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
                Ticket.createTicket(getGuildEntity(), channel, event.getMember(), null);
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

        if (ticketChannelEntity == null && event.getComponentId().equals(BUTTON_ID_CREATE)) {
            if (ticketsEntity.getEnforceModal()) {
                TextInput message = TextInput.create("message", getString("modal_message"), TextInputStyle.PARAGRAPH)
                        .setPlaceholder(getString("modal_message_placeholder"))
                        .setMinLength(30)
                        .setMaxLength(1000)
                        .build();

                Modal modal = Modal.create(ModalInteractionTicket.ID, getString("button_create"))
                        .addActionRows(ActionRow.of(message))
                        .build();

                event.replyModal(modal).queue();
            } else {
                Category category = channel.getParentCategory();
                if (category == null || category.getChannels().size() < 50) {
                    Ticket.createTicket(getGuildEntity(), channel, event.getMember(), null);
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                    event.replyEmbeds(eb.build())
                            .setEphemeral(true)
                            .queue();
                }
            }
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

    private boolean memberIsStaff(Member member, List<Long> staffRoleIds) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                staffRoleIds.stream().anyMatch(roleId -> member.getRoles().stream().anyMatch(r -> roleId == r.getIdLong()));
    }

}
