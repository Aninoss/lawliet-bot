package commands.runnables.utilitycategory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import commands.CommandEvent;
import commands.NavigationHelper;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnStaticButtonListener;
import commands.listeners.OnStaticReactionAddListener;
import commands.runnables.NavigationAbstract;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicRole;
import core.atomicassets.MentionableAtomicAsset;
import core.cache.ServerPatreonBoostCache;
import core.cache.TicketProtocolCache;
import core.components.ActionRows;
import core.interactionresponse.ComponentInteractionResponse;
import core.lock.Lock;
import core.lock.LockOccupiedException;
import core.utils.*;
import events.discordevents.modalinteraction.ModalInteractionTicket;
import kotlin.Pair;
import modules.Ticket;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Modal;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "ticket",
        botChannelPermissions = { Permission.MESSAGE_EXT_EMOJI },
        botGuildPermissions = { Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_ROLES },
        userGuildPermissions = { Permission.MANAGE_CHANNEL },
        releaseDate = { 2021, 5, 24 },
        emoji = "🎟️",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "tickets", "supportticket", "supporttickets" }
)
public class TicketCommand extends NavigationAbstract implements OnStaticReactionAddListener, OnStaticButtonListener {

    private final static int MAX_ROLES = 10;
    public final static UnicodeEmoji TICKET_CLOSE_EMOJI = Emojis.X;
    private final static int
            MAIN = 0,
            ANNOUNCEMENT_CHANNEL = 1,
            ADD_STAFF_ROLE = 2,
            REMOVE_STAFF_ROLE = 3,
            CREATE_TICKET_MESSAGE = 4,
            GREETING_TEXT = 5;
    public final static String BUTTON_ID_CREATE = "create";
    public final static String BUTTON_ID_CLOSE = "close";

    private TicketData ticketData;
    private NavigationHelper<AtomicRole> staffRoleNavigationHelper;
    private CustomObservableList<AtomicRole> staffRoles;
    private TextChannel tempPostChannel = null;

    public TicketCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        staffRoles = AtomicRole.transformIdList(event.getGuild(), ticketData.getStaffRoleIds());
        staffRoleNavigationHelper = new NavigationHelper<>(this, staffRoles, AtomicRole.class, MAX_ROLES, false);
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = ADD_STAFF_ROLE)
    public MessageInputResponse onMessageAddStaffRole(MessageReceivedEvent event, String input) {
        List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
        return staffRoleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), MAIN);
    }

    @ControllerMessage(state = ANNOUNCEMENT_CHANNEL)
    public MessageInputResponse onMessageAnnouncementChannel(MessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (channelList.size() == 0) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return MessageInputResponse.FAILED;
        } else {
            TextChannel textChannel = channelList.get(0);

            String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), textChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS);
            if (channelMissingPerms != null) {
                setLog(LogStatus.FAILURE, channelMissingPerms);
                return MessageInputResponse.FAILED;
            }

            ticketData.setAnnouncementTextChannelId(textChannel.getIdLong());
            setLog(LogStatus.SUCCESS, getString("announcement_set"));
            setState(MAIN);
            return MessageInputResponse.SUCCESS;
        }
    }

    @ControllerMessage(state = CREATE_TICKET_MESSAGE)
    public MessageInputResponse onMessageCreateTicketMessage(MessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (channelList.size() == 0) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return MessageInputResponse.FAILED;
        } else {
            tempPostChannel = channelList.get(0);
            return MessageInputResponse.SUCCESS;
        }
    }

    @ControllerMessage(state = GREETING_TEXT)
    public MessageInputResponse onMessageGreetingText(MessageReceivedEvent event, String input) {
        int max = 1000;
        if (input.length() > 0) {
            if (input.length() <= max) {
                ticketData.setCreateMessage(input);
                setLog(LogStatus.SUCCESS, getString("greetingset"));
                setState(0);
                return MessageInputResponse.SUCCESS;
            } else {
                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "too_many_characters", StringUtil.numToString(max)));
                return MessageInputResponse.FAILED;
            }
        }
        return MessageInputResponse.FAILED;
    }

    @ControllerButton(state = MAIN)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1:
                deregisterListenersWithComponentMessage();
                return false;

            case 0:
                setState(ANNOUNCEMENT_CHANNEL);
                return true;

            case 1:
                staffRoleNavigationHelper.startDataAdd(ADD_STAFF_ROLE);
                return true;

            case 2:
                staffRoleNavigationHelper.startDataRemove(REMOVE_STAFF_ROLE);
                return true;

            case 3:
                ticketData.togglePingStaff();
                setLog(LogStatus.SUCCESS, getString("boolean_set", ticketData.getPingStaff(), getString("state0_mping")));
                return true;

            case 4:
                ticketData.toggleMemberCanClose();
                setLog(LogStatus.SUCCESS, getString("boolean_set", ticketData.memberCanClose(), getString("state0_mmembercanclose")));
                return true;

            case 5:
                ticketData.toggleAssignToAll();
                setLog(LogStatus.SUCCESS, getString("boolean_set", ticketData.getAssignToAll(), getString("state0_massign")));
                return true;

            case 6:
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    ticketData.toggleProtocol();
                    setLog(LogStatus.SUCCESS, getString("boolean_set", ticketData.getProtocolEffectively(), getString("state0_mprotocol")));
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                }
                return true;

            case 7:
                ticketData.toggleUserMessages();
                setLog(LogStatus.SUCCESS, getString("boolean_set", ticketData.getUserMessages(), getString("state0_mtextinput")));
                return true;

            case 8:
                setState(GREETING_TEXT);
                return true;

            case 9:
                setState(CREATE_TICKET_MESSAGE);
                return true;

            default:
                return false;
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
                ticketData.setAnnouncementTextChannelId(null);
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
            setState(0);
            return true;
        }
        return false;
    }

    @ControllerButton(state = REMOVE_STAFF_ROLE)
    public boolean onButtonRemoveStaffRole(ButtonInteractionEvent event, int i) {
        return staffRoleNavigationHelper.removeData(i, MAIN);
    }

    @ControllerButton(state = CREATE_TICKET_MESSAGE)
    public boolean onButtonCreateTicketMessage(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        } else if (i == 0 && tempPostChannel != null) {
            tempPostChannel = tempPostChannel.getGuild().getTextChannelById(tempPostChannel.getIdLong());
            if (tempPostChannel != null) {
                String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), tempPostChannel,
                        Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY
                );
                if (channelMissingPerms != null) {
                    setLog(LogStatus.FAILURE, channelMissingPerms);
                    return true;
                }

                Category parent = tempPostChannel.getParentCategory();
                if (parent != null) {
                    String categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), parent, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL);
                    if (categoryMissingPerms != null) {
                        setLog(LogStatus.FAILURE, categoryMissingPerms);
                        return true;
                    }
                }

                String emoji = getCommandProperties().emoji();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("message_content", emoji));
                eb.setFooter(null);

                tempPostChannel.sendMessageEmbeds(eb.build())
                        .setActionRows(ActionRows.of(Button.of(ButtonStyle.PRIMARY, BUTTON_ID_CREATE, getString("button_create"))))
                        .queue(this::registerStaticReactionMessage);

                setLog(LogStatus.SUCCESS, getString("message_sent"));
                setState(MAIN);
            }
            return true;
        }
        return false;
    }

    @ControllerButton(state = GREETING_TEXT)
    public boolean onButtonGreetingText(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        } else if (i == 0) {
            ticketData.setCreateMessage(null);
            setLog(LogStatus.SUCCESS, getString("greetingset"));
            setState(0);
            return true;
        }
        return false;
    }

    @Draw(state = MAIN)
    public EmbedBuilder onDrawMain(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        setComponents(getString("state0_options").split("\n"));
        return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                .addField(getString("state0_mannouncement"), StringUtil.escapeMarkdown(ticketData.getAnnouncementTextChannel().map(GuildChannel::getAsMention).orElse(notSet)), true)
                .addField(getString("state0_mstaffroles"), new ListGen<AtomicRole>().getList(staffRoles, getLocale(), MentionableAtomicAsset::getAsMention), true)
                .addField(getString("state0_mproperties"), generateBooleanAttributesField(), false)
                .addField(getString("state0_mcreatemessage"), StringUtil.escapeMarkdown(ticketData.getCreateMessage().orElse(notSet)), false);
    }

    private String generateBooleanAttributesField() {
        List<Pair<String, Boolean>> attributesList = List.of(
                new Pair<>(getString("state0_mping"), ticketData.getPingStaff()),
                new Pair<>(getString("state0_mmembercanclose"), ticketData.memberCanClose()),
                new Pair<>(getString("state0_massign"), ticketData.getAssignToAll()),
                new Pair<>(getString("state0_mprotocol") + " " + Emojis.COMMAND_ICON_PREMIUM.getFormatted(), ticketData.getProtocolEffectively()),
                new Pair<>(getString("state0_mtextinput"), ticketData.getUserMessages())
        );

        StringBuilder sb = new StringBuilder();
        for (Pair<String, Boolean> attribute : attributesList) {
            sb.append("• ")
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
        return staffRoleNavigationHelper.drawDataRemove(getString("state3_title"), getString("state3_description"));
    }

    @Draw(state = CREATE_TICKET_MESSAGE)
    public EmbedBuilder onDrawCreateTicketMessage(Member member) {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        if (tempPostChannel != null) {
            setComponents(getString("state4_options").split("\n"));
        }
        return EmbedFactory.getEmbedDefault(
                this,
                getString("state4_description", tempPostChannel != null ? tempPostChannel.getAsMention() : notSet),
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
        if (event.getChannel() instanceof TextChannel) {
            TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
            TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());

            if (ticketChannel == null && event.getEmoji().getFormatted().equals(getCommandProperties().emoji())) {
                Category category = event.getChannel().asTextChannel().getParentCategory();
                if (category == null || category.getTextChannels().size() < 50) {
                    Ticket.createTicket(ticketData, event.getChannel().asTextChannel(), event.getMember(), null);
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                    JDAUtil.openPrivateChannel(event.getMember())
                            .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                            .queue();
                }
            } else if (ticketChannel != null && EmojiUtil.equals(event.getEmoji(), TICKET_CLOSE_EMOJI)) {
                boolean isStaff = memberIsStaff(event.getMember(), ticketData.getStaffRoleIds());
                if (isStaff || ticketData.memberCanClose()) {
                    onTicketRemove(ticketData, event.getChannel().asTextChannel());
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                    event.getChannel().asTextChannel().sendMessageEmbeds(eb.build())
                            .queue();
                }
            }
        }
    }

    @Override
    public void onStaticButton(ButtonInteractionEvent event) {
        if (event.getChannel() instanceof TextChannel) {
            TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
            TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());

            if (ticketChannel == null && event.getComponentId().equals(BUTTON_ID_CREATE)) {
                if (ticketData.getUserMessages()) {
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
                    Category category = event.getChannel().asTextChannel().getParentCategory();
                    if (category == null || category.getTextChannels().size() < 50) {
                        Ticket.createTicket(ticketData, event.getChannel().asTextChannel(), event.getMember(), null);
                    } else {
                        EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                        event.replyEmbeds(eb.build())
                                .setEphemeral(true)
                                .queue();
                    }
                }
            } else if (ticketChannel != null && event.getComponentId().equals(BUTTON_ID_CLOSE)) {
                boolean isStaff = memberIsStaff(event.getMember(), ticketData.getStaffRoleIds());
                if (isStaff || ticketData.memberCanClose()) {
                    onTicketRemove(ticketData, event.getChannel().asTextChannel());
                } else {
                    EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                    event.replyEmbeds(eb.build())
                            .setEphemeral(true)
                            .queue();
                }
            }
            new ComponentInteractionResponse(event).complete();
        }
    }

    private boolean memberIsStaff(Member member, List<Long> staffRoleIds) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                staffRoleIds.stream().anyMatch(roleId -> member.getRoles().stream().anyMatch(r -> roleId == r.getIdLong()));
    }

    private void onTicketRemove(TicketData ticketData, TextChannel channel) {
        AuditableRestAction<Void> channelDeleteRestAction = channel.delete()
                .reason(getCommandLanguage().getTitle());

        if (ticketData.getProtocolEffectively()) {
            GlobalThreadPool.getExecutorService().submit(() -> {
                try (Lock lock = new Lock(TicketCommand.class)) {
                    MessageHistory messageHistory = channel.getHistory();
                    List<Message> messageLoadList;
                    do {
                        messageLoadList = messageHistory.retrievePast(100).complete();
                    } while (messageLoadList.size() == 100);

                    ArrayList<Message> messageList = new ArrayList<>(messageHistory.getRetrievedHistory());
                    Collections.reverse(messageList);

                    ArrayList<String[]> csvRows = new ArrayList<>();
                    csvRows.add(getString("csv_titles").split("\n"));
                    DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                            .withLocale(Locale.US);
                    long lastAuthorId = 0L;
                    Instant lastMessageTime = null;
                    for (Message message : messageList) {
                        String contentRaw = extractContentFromMessage(message);
                        if (contentRaw.length() > 0 ||
                                message.getAttachments().size() > 0
                        ) {
                            String content = WordUtils.wrap(contentRaw, 100);
                            String[] row = new String[] { " ", " ", content.length() > 0 ? content : " ", " " };

                            if (message.getAuthor().getIdLong() != lastAuthorId ||
                                    lastMessageTime == null ||
                                    message.getTimeCreated().toInstant().isAfter(lastMessageTime.plus(Duration.ofMinutes(15)))
                            ) {
                                row[0] = formatter.format(message.getTimeCreated());
                                row[1] = message.getAuthor().getAsTag();
                            }

                            if (message.getAttachments().size() > 0) {
                                StringBuilder attachments = new StringBuilder();
                                for (Message.Attachment attachment : message.getAttachments()) {
                                    if (attachments.length() > 0) {
                                        attachments.append("\n");
                                    }
                                    attachments.append(attachment.getUrl());
                                }
                                row[3] = attachments.toString();
                            }

                            lastAuthorId = message.getAuthor().getIdLong();
                            lastMessageTime = message.getTimeCreated().toInstant();
                            csvRows.add(row);
                        }
                    }

                    LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("tickets/%s.csv", RandomUtil.generateRandomString(30)));
                    try (InputStream is = CSVGenerator.generateInputStream(csvRows)) {
                        FileUtil.writeInputStreamToFile(is, tempFile);
                    } catch (IOException e) {
                        MainLogger.get().error("Error", e);
                    }
                    TicketProtocolCache.setUrl(channel.getIdLong(), tempFile.cdnGetUrl());
                    channelDeleteRestAction.queue();
                } catch (LockOccupiedException e) {
                    //Ignore
                }
            });
        } else {
            channelDeleteRestAction.queue();
        }
    }

    private String extractContentFromMessage(Message message) {
        String content = message.getContentDisplay();
        if (message.getEmbeds().size() > 0 &&
                message.getEmbeds().get(0).getDescription() != null &&
                message.getEmbeds().get(0).getDescription().length() > 0
        ) {
            MessageEmbed messageEmbed = message.getEmbeds().get(0);
            if (messageEmbed.getAuthor() != null &&
                    messageEmbed.getAuthor().getName() != null
            ) {
                content = getString("csv_author",
                        messageEmbed.getAuthor().getName(),
                        messageEmbed.getDescription()
                );
            } else {
                content = messageEmbed.getDescription();
            }
        }
        return content;
    }

}
