package commands.runnables.utilitycategory;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
import modules.Ticket;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import mysql.modules.ticket.TicketData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;

@CommandProperties(
        trigger = "ticket",
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
    private final static String TICKET_CLOSE_EMOJI = Emojis.X;
    private final static int
            MAIN = 0,
            ANNOUNCEMENT_CHANNEL = 1,
            ADD_STAFF_ROLE = 2,
            REMOVE_STAFF_ROLE = 3,
            CREATE_TICKET_MESSAGE = 4,
            GREETING_TEXT = 5;
    private final static String BUTTON_ID_CREATE = "create";
    private final static String BUTTON_ID_CLOSE = "close";

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
        staffRoleNavigationHelper = new NavigationHelper<>(this, staffRoles, AtomicRole.class, MAX_ROLES);
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = ADD_STAFF_ROLE)
    public MessageInputResponse onMessageAddStaffRole(GuildMessageReceivedEvent event, String input) {
        List<Role> roleList = MentionUtil.getRoles(event.getGuild(), input).getList();
        return staffRoleNavigationHelper.addData(AtomicRole.from(roleList), input, event.getMember(), MAIN);
    }

    @ControllerMessage(state = ANNOUNCEMENT_CHANNEL)
    public MessageInputResponse onMessageAnnouncementChannel(GuildMessageReceivedEvent event, String input) {
        List<TextChannel> channelList = MentionUtil.getTextChannels(event.getGuild(), input).getList();
        if (channelList.size() == 0) {
            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
            return MessageInputResponse.FAILED;
        } else {
            TextChannel textChannel = channelList.get(0);

            String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), textChannel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS);
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
    public MessageInputResponse onMessageCreateTicketMessage(GuildMessageReceivedEvent event, String input) {
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
    public MessageInputResponse onMessageGreetingText(GuildMessageReceivedEvent event, String input) {
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
    public boolean onButtonMain(ButtonClickEvent event, int i) {
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
                setLog(LogStatus.SUCCESS, getString("ping_set", ticketData.getPingStaff()));
                return true;

            case 4:
                ticketData.toggleMemberCanClose();
                setLog(LogStatus.SUCCESS, getString("membercanclose_set", ticketData.memberCanClose()));
                return true;

            case 5:
                ticketData.toggleAssignToAll();
                setLog(LogStatus.SUCCESS, getString("assign_set", ticketData.getAssignToAll()));
                return true;

            case 6:
                if (ServerPatreonBoostCache.get(event.getGuild().getIdLong())) {
                    ticketData.toggleProtocol();
                    setLog(LogStatus.SUCCESS, getString("protocol_set", ticketData.getProtocolEffectively()));
                } else {
                    setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_unlock"));
                }
                return true;

            case 7:
                setState(GREETING_TEXT);
                return true;

            case 8:
                setState(CREATE_TICKET_MESSAGE);
                return true;

            default:
                return false;
        }
    }

    @ControllerButton(state = ANNOUNCEMENT_CHANNEL)
    public boolean onButtonAnnouncementChannel(ButtonClickEvent event, int i) {
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
    public boolean onButtonAddStaffRole(ButtonClickEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        }
        return false;
    }

    @ControllerButton(state = REMOVE_STAFF_ROLE)
    public boolean onButtonRemoveStaffRole(ButtonClickEvent event, int i) {
        return staffRoleNavigationHelper.removeData(i, MAIN);
    }

    @ControllerButton(state = CREATE_TICKET_MESSAGE)
    public boolean onButtonCreateTicketMessage(ButtonClickEvent event, int i) {
        if (i == -1) {
            setState(0);
            return true;
        } else if (i == 0 && tempPostChannel != null) {
            tempPostChannel = tempPostChannel.getGuild().getTextChannelById(tempPostChannel.getIdLong());
            if (tempPostChannel != null) {
                String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), tempPostChannel,
                        Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY
                );
                if (channelMissingPerms != null) {
                    setLog(LogStatus.FAILURE, channelMissingPerms);
                    return true;
                }

                Category parent = tempPostChannel.getParent();
                if (parent != null) {
                    String categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(getLocale(), parent, Permission.VIEW_CHANNEL, Permission.MESSAGE_WRITE, Permission.MANAGE_CHANNEL);
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
    public boolean onButtonGreetingText(ButtonClickEvent event, int i) {
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
                .addField(getString("state0_mping"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), ticketData.getPingStaff()), true)
                .addField(getString("state0_mmembercanclose"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), ticketData.memberCanClose()), true)
                .addField(getString("state0_massign"), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), ticketData.getAssignToAll()), true)
                .addField(getString("state0_mprotocol", Emojis.COMMAND_ICON_PREMIUM), StringUtil.getOnOffForBoolean(getTextChannel().get(), getLocale(), ticketData.getProtocolEffectively()), true)
                .addField(getString("state0_mcreatemessage"), StringUtil.escapeMarkdown(ticketData.getCreateMessage().orElse(notSet)), false);
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
    public void onStaticReactionAdd(@NotNull Message message, @NotNull GuildMessageReactionAddEvent event) throws Throwable {
        TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());

        if (ticketChannel == null && EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), getCommandProperties().emoji())) {
            if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                message.removeReaction(getCommandProperties().emoji(), event.getUser()).queue();
            }
            Category category = event.getChannel().getParent();
            if (category != null && category.getTextChannels().size() < 50) {
                onTicketCreate(ticketData, event.getChannel(), event.getMember());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                JDAUtil.sendPrivateMessage(event.getMember(), eb.build())
                        .queue();
            }
        } else if (ticketChannel != null && EmojiUtil.reactionEmoteEqualsEmoji(event.getReactionEmote(), TICKET_CLOSE_EMOJI)) {
            boolean isStaff = memberIsStaff(event.getMember(), ticketData.getStaffRoleIds());
            if (isStaff || ticketData.memberCanClose()) {
                onTicketRemove(ticketData, event.getChannel());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                event.getChannel().sendMessageEmbeds(eb.build())
                        .queue();
            }
        }
    }

    @Override
    public void onStaticButton(ButtonClickEvent event) {
        TicketData ticketData = DBTicket.getInstance().retrieve(event.getGuild().getIdLong());
        TicketChannel ticketChannel = ticketData.getTicketChannels().get(event.getChannel().getIdLong());

        if (ticketChannel == null && event.getComponentId().equals(BUTTON_ID_CREATE)) {
            Category category = event.getTextChannel().getParent();
            if (category != null && category.getTextChannels().size() < 50) {
                onTicketCreate(ticketData, event.getTextChannel(), event.getMember());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("toomanychannels"));
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        } else if (ticketChannel != null && event.getComponentId().equals(BUTTON_ID_CLOSE)) {
            boolean isStaff = memberIsStaff(event.getMember(), ticketData.getStaffRoleIds());
            if (isStaff || ticketData.memberCanClose()) {
                onTicketRemove(ticketData, event.getTextChannel());
            } else {
                EmbedBuilder eb = EmbedFactory.getEmbedError(this, getString("cannotclose"));
                event.replyEmbeds(eb.build())
                        .setEphemeral(true)
                        .queue();
            }
        }
        new ComponentInteractionResponse(event).complete();
    }

    private boolean memberIsStaff(Member member, List<Long> staffRoleIds) {
        return BotPermissionUtil.can(member, Permission.ADMINISTRATOR) ||
                staffRoleIds.stream().anyMatch(roleId -> member.getRoles().stream().anyMatch(r -> roleId == r.getIdLong()));
    }

    private void onTicketCreate(TicketData ticketData, TextChannel channel, Member member) {
        Optional<TextChannel> existingTicketChannelOpt = findTicketChannelOfUser(ticketData, member);
        if (existingTicketChannelOpt.isEmpty()) {
            Ticket.createTicketChannel(channel, member, ticketData).ifPresent(channelAction -> {
                channelAction.queue(textChannel -> setupNewTicketChannel(ticketData, textChannel, member));
            });
        } else {
            TextChannel existingTicketChannel = existingTicketChannelOpt.get();
            if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), getClass(), existingTicketChannel, Permission.MESSAGE_WRITE)) {
                existingTicketChannel.sendMessage(member.getAsMention()).queue();
            }
        }
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
                        if (message.getContentDisplay().length() > 0 || message.getAttachments().size() > 0) {
                            String content = WordUtils.wrap(message.getContentDisplay(), 100);
                            String[] row = new String[] { " ", " ", content.length() > 0 ? content : " ", " " };

                            if (message.getAuthor().getIdLong() != lastAuthorId || lastMessageTime == null ||
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

    private void setupNewTicketChannel(TicketData ticketData, TextChannel textChannel, Member member) {
        /* member greeting */
        EmbedBuilder eb = EmbedFactory.getEmbedDefault(this, getString("greeting", TICKET_CLOSE_EMOJI));
        textChannel.sendMessageEmbeds(eb.build())
                .setActionRows(ActionRows.of(Button.of(ButtonStyle.DANGER, BUTTON_ID_CLOSE, getString("button_close"))))
                .content(member.getAsMention())
                .queue(this::registerStaticReactionMessage);

        /* create message */
        ticketData.getCreateMessage().ifPresent(createMessage -> {
            if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), getClass(), textChannel, Permission.MESSAGE_WRITE)) {
                textChannel.sendMessage(createMessage)
                        .allowedMentions(null)
                        .queue();
            }
        });

        /* post announcement to staff channel */
        AtomicBoolean announcementNotPosted = new AtomicBoolean(true);
        ticketData.getAnnouncementTextChannel().ifPresent(announcementChannel -> {
            if (PermissionCheckRuntime.botHasPermission(ticketData.getGuildData().getLocale(), getClass(), announcementChannel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
                announcementNotPosted.set(false);
                EmbedBuilder ebAnnouncement = EmbedFactory.getEmbedDefault(this, getString("announcement_open", member.getAsMention(), textChannel.getAsMention()));
                announcementChannel.sendMessage(ticketData.getPingStaff() ? getRolePing(textChannel.getGuild(), ticketData) : " ")
                        .setEmbeds(ebAnnouncement.build())
                        .allowedMentions(Collections.singleton(Message.MentionType.ROLE))
                        .queue(m -> {
                            ticketData.getTicketChannels().put(textChannel.getIdLong(), new TicketChannel(
                                    textChannel.getGuild().getIdLong(),
                                    textChannel.getIdLong(),
                                    member.getIdLong(),
                                    announcementChannel.getIdLong(),
                                    m.getIdLong(),
                                    false
                            ));
                        }, e -> {
                            MainLogger.get().error("Ticket announcement error", e);
                            ticketData.getTicketChannels().put(textChannel.getIdLong(), new TicketChannel(
                                    textChannel.getGuild().getIdLong(),
                                    textChannel.getIdLong(),
                                    member.getIdLong(),
                                    0L,
                                    0L,
                                    false
                            ));
                        });
            }
        });

        if (announcementNotPosted.get()) {
            ticketData.getTicketChannels().put(textChannel.getIdLong(), new TicketChannel(
                    textChannel.getGuild().getIdLong(),
                    textChannel.getIdLong(),
                    member.getIdLong(),
                    0L,
                    0L,
                    false
            ));
        }
    }

    private Optional<TextChannel> findTicketChannelOfUser(TicketData ticketData, Member member) {
        for (TicketChannel ticketChannel : ticketData.getTicketChannels().values()) {
            if (ticketChannel.getMemberId() == member.getIdLong()) {
                Optional<TextChannel> textChannelOpt = ticketChannel.getTextChannel();
                if (textChannelOpt.isPresent()) {
                    return textChannelOpt;
                }
            }
        }
        return Optional.empty();
    }

    private String getRolePing(Guild guild, TicketData ticketData) {
        StringBuilder pings = new StringBuilder();
        ticketData.getStaffRoleIds()
                .transform(guild::getRoleById, ISnowflake::getIdLong)
                .forEach(role -> {
                    pings.append(role.getAsMention()).append(" ");
                });

        if (pings.isEmpty()) {
            return " ";
        }
        return pings.toString();
    }

}
