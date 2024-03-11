package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnStaticButtonListener;
import commands.listeners.OnStaticReactionAddListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.TextManager;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicTextChannel;
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
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

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

    private final static int
            MAIN = 0,
            ANNOUNCEMENT_CHANNEL = 1,
            ADD_STAFF_ROLE = 2,
            REMOVE_STAFF_ROLE = 3,
            CREATE_TICKET_MESSAGE = 4,
            GREETING_TEXT = 5,
            ASSIGNMENT_MODE = 6,
            CLOSE_ON_INACTIVITY = 7;
    public final static String BUTTON_ID_CREATE = "create";
    public final static String BUTTON_ID_CLOSE = "close";
    public final static String BUTTON_ID_ASSIGN = "assign";

    private NavigationHelper<AtomicRole> staffRoleNavigationHelper;
    private TextChannel tempPostChannel = null;

    public TicketCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        staffRoleNavigationHelper = new NavigationHelper<>(this, guildEntity -> guildEntity.getTickets().getStaffRoles(), AtomicRole.class, MAX_STAFF_ROLES, false);
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = ADD_STAFF_ROLE)
    public MessageInputResponse onMessageAddStaffRole(MessageReceivedEvent event, String input) {
        List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
        return staffRoleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), MAIN, BotLogEntity.Event.TICKETS_STAFF_ROLES);
    }

    @ControllerMessage(state = ANNOUNCEMENT_CHANNEL)
    public MessageInputResponse onMessageAnnouncementChannel(MessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (channelList.isEmpty()) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return MessageInputResponse.FAILED;
        } else {
            TextChannel textChannel = channelList.get(0);

            String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), textChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS);
            if (channelMissingPerms != null) {
                setLog(LogStatus.FAILURE, channelMissingPerms);
                return MessageInputResponse.FAILED;
            }

            TicketsEntity tickets = getGuildEntity().getTickets();
            tickets.beginTransaction();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_LOG_CHANNEL, event.getMember(), tickets.getLogChannelId(), textChannel.getIdLong());
            tickets.setLogChannelId(textChannel.getIdLong());
            tickets.commitTransaction();

            setLog(LogStatus.SUCCESS, getString("announcement_set"));
            setState(MAIN);
            return MessageInputResponse.SUCCESS;
        }
    }

    @ControllerMessage(state = CLOSE_ON_INACTIVITY)
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
            setState(MAIN);
            return MessageInputResponse.SUCCESS;
        } else {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "invalid", input));
            return MessageInputResponse.FAILED;
        }
    }

    @ControllerMessage(state = CREATE_TICKET_MESSAGE)
    public MessageInputResponse onMessageCreateTicketMessage(MessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (channelList.isEmpty()) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return MessageInputResponse.FAILED;
        } else {
            tempPostChannel = channelList.get(0);
            return MessageInputResponse.SUCCESS;
        }
    }

    @ControllerMessage(state = GREETING_TEXT)
    public MessageInputResponse onMessageGreetingText(MessageReceivedEvent event, String input) {
        if (!input.isEmpty()) {
            if (input.length() <= MAX_GREETING_TEXT_LENGTH) {
                TicketsEntity tickets = getGuildEntity().getTickets();
                tickets.beginTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_GREETING_TEXT, event.getMember(), tickets.getGreetingText(), input);
                tickets.setGreetingText(input);
                tickets.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("greetingset"));
                setState(0);
                return MessageInputResponse.SUCCESS;
            } else {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(MAX_GREETING_TEXT_LENGTH)));
                return MessageInputResponse.FAILED;
            }
        }
        return MessageInputResponse.FAILED;
    }

    @ControllerButton(state = MAIN)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        TicketsEntity tickets = getGuildEntity().getTickets();

        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                setState(ANNOUNCEMENT_CHANNEL);
                return true;
            }
            case 1 -> {
                staffRoleNavigationHelper.startDataAdd(ADD_STAFF_ROLE);
                return true;
            }
            case 2 -> {
                staffRoleNavigationHelper.startDataRemove(REMOVE_STAFF_ROLE);
                return true;
            }
            case 3 -> {
                setState(ASSIGNMENT_MODE);
                return true;
            }
            case 4 -> {
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    setState(CLOSE_ON_INACTIVITY);
                    return true;
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                }
                return true;
            }
            case 5 -> {
                setState(GREETING_TEXT);
                return true;
            }
            case 6 -> {
                tickets.beginTransaction();
                tickets.setPingStaffRoles(!tickets.getPingStaffRoles());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_PING_STAFF_ROLES, event.getMember(), null, tickets.getPingStaffRoles());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getPingStaffRoles(), getString("state0_mping")));
                return true;
            }
            case 7 -> {
                tickets.beginTransaction();
                tickets.setEnforceModal(!tickets.getEnforceModal());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_ENFORCE_MODAL, event.getMember(), null, tickets.getEnforceModal());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getEnforceModal(), getString("state0_mtextinput")));
                return true;
            }
            case 8 -> {
                tickets.beginTransaction();
                tickets.setMembersCanCloseTickets(!tickets.getMembersCanCloseTickets());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_MEMBERS_CAN_CLOSE_TICKETS, event.getMember(), null, tickets.getMembersCanCloseTickets());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getMembersCanCloseTickets(), getString("state0_mmembercanclose")));
                return true;
            }
            case 9 -> {
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
            case 10 -> {
                tickets.beginTransaction();
                tickets.setDeleteChannelsOnClose(!tickets.getDeleteChannelsOnClose());
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_DELETE_CHANNELS_ON_CLOSE, event.getMember(), null, tickets.getDeleteChannelsOnClose());
                tickets.commitTransaction();
                setLog(LogStatus.SUCCESS, getString("boolean_set", tickets.getDeleteChannelsOnClose(), getString("state0_mdeletechannel")));
                return true;
            }
            case 11 -> {
                setState(CREATE_TICKET_MESSAGE);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    @ControllerButton(state = ANNOUNCEMENT_CHANNEL)
    public boolean onButtonAnnouncementChannel(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                setState(MAIN);
                return true;
            }
            case 0 -> {
                TicketsEntity tickets = getGuildEntity().getTickets();
                tickets.beginTransaction();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_LOG_CHANNEL, event.getMember(), tickets.getLogChannelId(), null);
                tickets.setLogChannelId(null);
                tickets.commitTransaction();

                setLog(LogStatus.SUCCESS, getString("announcement_set"));
                setState(MAIN);
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = ADD_STAFF_ROLE)
    public boolean onButtonAddStaffRole(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(MAIN);
            return true;
        }
        return false;
    }

    @ControllerButton(state = REMOVE_STAFF_ROLE)
    public boolean onButtonRemoveStaffRole(ButtonInteractionEvent event, int i) {
        return staffRoleNavigationHelper.removeData(i, event.getMember(), MAIN, BotLogEntity.Event.TICKETS_STAFF_ROLES);
    }

    @ControllerButton(state = ASSIGNMENT_MODE)
    public boolean onButtonAssignmentMode(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(MAIN);
            return true;
        }

        TicketsEntity tickets = getGuildEntity().getTickets();
        tickets.beginTransaction();
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_ASSIGNMENT_MODE, event.getMember(), tickets.getAssignmentMode(), TicketsEntity.AssignmentMode.values()[i]);
        tickets.setAssignmentMode(TicketsEntity.AssignmentMode.values()[i]);
        tickets.commitTransaction();

        setLog(LogStatus.SUCCESS, getString("assignment_set", getString("assignment_modes").split("\n")[i]));
        setState(MAIN);
        return true;
    }

    @ControllerButton(state = CLOSE_ON_INACTIVITY)
    public boolean onButtonCloseOnInactivity(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(MAIN);
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
            setState(MAIN);
            return true;
        }
        return false;
    }

    @ControllerButton(state = CREATE_TICKET_MESSAGE)
    public boolean onButtonCreateTicketMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        } else if (i == 0 && tempPostChannel != null) {
            tempPostChannel = tempPostChannel.getGuild().getTextChannelById(tempPostChannel.getIdLong());
            if (tempPostChannel != null) {
                String error = Ticket.sendTicketMessage(getGuildEntity(), getLocale(), tempPostChannel);
                if (error == null) {
                    getEntityManager().getTransaction().begin();
                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_CREATE_TICKET_MESSAGE, event.getMember(), tempPostChannel.getId());
                    getEntityManager().getTransaction().commit();

                    setLog(LogStatus.SUCCESS, getString("message_sent"));
                    setState(MAIN);
                } else {
                    setLog(LogStatus.FAILURE, error);
                }
                return true;
            }
        }
        return false;
    }

    @ControllerButton(state = GREETING_TEXT)
    public boolean onButtonGreetingText(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        } else if (i == 0) {
            TicketsEntity tickets = getGuildEntity().getTickets();
            tickets.beginTransaction();
            BotLogEntity.log(getEntityManager(), BotLogEntity.Event.TICKETS_GREETING_TEXT, event.getMember(), tickets.getGreetingText(), null);
            tickets.setGreetingText(null);
            tickets.commitTransaction();

            setLog(LogStatus.SUCCESS, getString("greetingset"));
            setState(0);
            return true;
        }
        return false;
    }

    @Draw(state = MAIN)
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
            return StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), false);
        } else {
            return TimeUtil.getRemainingTimeString(getLocale(), autoCloseMinutes * 3_600_000, false);
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
                    .append(StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), attribute.getSecond()))
                    .append("\n");
        }
        return sb.toString();
    }

    @Draw(state = ANNOUNCEMENT_CHANNEL)
    public EmbedBuilder onDrawAnnouncementChannel(Member member) {
        setComponents(getString("state1_options").split("\n"));
        return staffRoleNavigationHelper.drawDataAdd(getString("state1_title"), getString("state1_description"));
    }

    @Draw(state = ADD_STAFF_ROLE)
    public EmbedBuilder onDrawAddStaffRole(Member member) {
        return staffRoleNavigationHelper.drawDataAdd(getString("state2_title"), getString("state2_description"));
    }

    @Draw(state = REMOVE_STAFF_ROLE)
    public EmbedBuilder onDrawRemoveStaffRole(Member member) {
        return staffRoleNavigationHelper.drawDataRemove(getString("state3_title"), getString("state3_description"), getLocale());
    }

    @Draw(state = ASSIGNMENT_MODE)
    public EmbedBuilder onDrawAssignmentMode(Member member) {
        setComponents(getString("assignment_modes").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state6_description"),
                getString("state6_title")
        );
    }

    @Draw(state = CLOSE_ON_INACTIVITY)
    public EmbedBuilder onDrawCloseOnInactivity(Member member) {
        setComponents(getString("state7_options").split("\n"));
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state7_description"),
                getString("state7_title")
        );
    }

    @Draw(state = CREATE_TICKET_MESSAGE)
    public EmbedBuilder onDrawCreateTicketMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (tempPostChannel != null) {
            setComponents(getString("state4_options").split("\n"));
        }
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state4_description", tempPostChannel != null ? new AtomicTextChannel(tempPostChannel).getPrefixedNameInField(getLocale()) : notSet),
                getString("state4_title")
        );
    }

    @Draw(state = GREETING_TEXT)
    public EmbedBuilder onDrawGreetingTextx(Member member) {
        setComponents(getString("state5_options").split("\n"));
        return staffRoleNavigationHelper.drawDataAdd(getString("state5_title"), getString("state5_description"));
    }

    @Override
    public void onStaticReactionAdd(@NotNull Message message, @NotNull MessageReactionAddEvent event) {
        if (!(event.getChannel() instanceof TextChannel)) {
            return;
        }

        TicketsEntity ticketsEntity = getGuildEntity().getTickets();
        TicketChannelEntity ticketChannelEntity = ticketsEntity.getTicketChannels().get(event.getChannel().getIdLong());

        if (ticketChannelEntity == null && event.getEmoji().getFormatted().equals(getCommandProperties().emoji())) {
            Category category = event.getChannel().asTextChannel().getParentCategory();
            if (category == null || category.getTextChannels().size() < 50) {
                Ticket.createTicket(getGuildEntity(), event.getChannel().asTextChannel(), event.getMember(), null);
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

                Ticket.closeTicket(getGuildEntity(), ticketChannelEntity, event.getChannel().asTextChannel(), event.getMember());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                event.getChannel().asTextChannel().sendMessageEmbeds(eb.build())
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

        if (!(channelTemp instanceof TextChannel)) {
            return;
        }

        TicketsEntity ticketsEntity = getGuildEntity().getTickets();
        TextChannel channel = (TextChannel) channelTemp;
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
                if (category == null || category.getTextChannels().size() < 50) {
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
