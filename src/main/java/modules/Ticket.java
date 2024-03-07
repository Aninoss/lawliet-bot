package modules;

import commands.Category;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.configurationcategory.TicketCommand;
import constants.Emojis;
import constants.LogStatus;
import core.*;
import core.atomicassets.AtomicRole;
import core.atomicassets.AtomicUser;
import core.cache.TicketProtocolCache;
import core.components.ActionRows;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.lock.Lock;
import core.lock.LockOccupiedException;
import core.utils.*;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import mysql.hibernate.entity.guild.TicketsEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import org.apache.commons.text.WordUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Ticket {

    public static String sendTicketMessage(Locale locale, TextChannel textChannel) {
        String channelMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, textChannel,
                Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY
        );
        if (channelMissingPerms != null) {
            return channelMissingPerms;
        }

        net.dv8tion.jda.api.entities.channel.concrete.Category parent = textChannel.getParentCategory();
        if (parent != null) {
            String categoryMissingPerms = BotPermissionUtil.getBotPermissionsMissingText(locale, parent, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL);
            if (categoryMissingPerms != null) {
                return categoryMissingPerms;
            }
        }

        String emoji = Command.getCommandProperties(TicketCommand.class).emoji();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(emoji + " " + Command.getCommandLanguage(TicketCommand.class, locale).getTitle())
                .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "ticket_message_content"));

        textChannel.sendMessageEmbeds(eb.build())
                .setComponents(ActionRows.of(Button.of(ButtonStyle.PRIMARY, TicketCommand.BUTTON_ID_CREATE, TextManager.getString(locale, Category.CONFIGURATION, "ticket_button_create"))))
                .queue(message -> {
                    DBStaticReactionMessages.getInstance()
                            .retrieve(message.getGuild().getIdLong())
                            .put(message.getIdLong(), new StaticReactionMessageData(message, Command.getCommandProperties(TicketCommand.class).trigger()));
                });

        return null;
    }

    public static void createTicket(GuildEntity guildEntity, TextChannel channel, Member member, String userMessage) {
        Locale locale = guildEntity.getLocale();
        TicketsEntity ticketsEntity = guildEntity.getTickets();
        Optional<TextChannel> existingTicketChannelOpt = findTicketChannelOfUser(ticketsEntity, member);

        if (existingTicketChannelOpt.isEmpty()) {
            TextChannel textChannel = Ticket.createTicketChannel(ticketsEntity, channel, member, locale);
            if (textChannel != null) {
                setupNewTicketChannel(ticketsEntity, textChannel, member, userMessage, guildEntity.getLocale());
            }
        } else {
            TextChannel existingTicketChannel = existingTicketChannelOpt.get();
            if (PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, existingTicketChannel, Permission.MESSAGE_SEND)) {
                String text = TextManager.getString(locale, commands.Category.CONFIGURATION, "ticket_alreadyopen", member.getAsMention());
                existingTicketChannel.sendMessage(text).queue();
            }
        }
    }

    private static Optional<TextChannel> findTicketChannelOfUser(TicketsEntity ticketsEntity, Member member) {
        for (TicketChannelEntity ticketChannelEntity : ticketsEntity.getTicketChannels().values()) {
            if (ticketChannelEntity.getMemberId() == member.getIdLong()) {
                Optional<TextChannel> textChannelOpt = ShardManager.getLocalGuildById(ticketsEntity.getGuildId())
                        .map(guild -> guild.getTextChannelById(ticketChannelEntity.getChannelId()));
                if (textChannelOpt.isPresent()) {
                    return textChannelOpt;
                }
            }
        }
        return Optional.empty();
    }

    private static void setupNewTicketChannel(TicketsEntity ticketsEntity, TextChannel textChannel, Member member, String userMessage, Locale locale) {
        CommandProperties commandProperties = Command.getCommandProperties(TicketCommand.class);
        String title = commandProperties.emoji() + " " + Command.getCommandLanguage(TicketCommand.class, locale).getTitle();

        long starterMessageId = 0;
        if (PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, textChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            /* member greeting */
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(title)
                    .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "ticket_greeting", TicketCommand.TICKET_CLOSE_EMOJI.getFormatted()));

            try {
                Message starterMessage = textChannel.sendMessageEmbeds(eb.build())
                        .setComponents(generateButtons(locale, ticketsEntity.getAssignmentMode() == TicketsEntity.AssignmentMode.MANUAL))
                        .setContent(member.getAsMention())
                        .submit()
                        .get();
                starterMessageId = starterMessage.getIdLong();
                DBStaticReactionMessages.getInstance()
                        .retrieve(textChannel.getGuild().getIdLong())
                        .put(starterMessage.getIdLong(), new StaticReactionMessageData(starterMessage, commandProperties.trigger()));
            } catch (InterruptedException | ExecutionException e) {
                MainLogger.get().error("Starter message could not be sent for ticket in guild {}", textChannel.getGuild().getIdLong());
            }

            /* user message */
            if (userMessage != null) {
                EmbedBuilder userMessageEmbed = EmbedFactory.getEmbedDefault()
                        .setDescription(userMessage);
                userMessageEmbed = EmbedUtil.setMemberAuthor(userMessageEmbed, member);
                textChannel.sendMessageEmbeds(userMessageEmbed.build())
                        .queue();
            }

            /* create message */
            if (ticketsEntity.getGreetingText() != null &&
                    PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, textChannel, Permission.MESSAGE_SEND)
            ) {
                textChannel.sendMessage(ticketsEntity.getGreetingText())
                        .addEmbeds(EmbedFactory.getWrittenByServerStaffEmbed(locale).build())
                        .setAllowedMentions(null)
                        .queue();
            }
        }

        /* post announcement to staff channel */
        long finalStarterMessageId = starterMessageId;
        TicketsEntity.AssignmentMode assignmentMode = ticketsEntity.getAssignmentMode();
        AtomicBoolean announcementNotPosted = new AtomicBoolean(true);

        ticketsEntity.getLogChannel().get().ifPresent(logChannel -> {
            if (PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, logChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
                announcementNotPosted.set(false);
                EmbedBuilder ebAnnouncement = EmbedFactory.getEmbedDefault()
                        .setTitle(title)
                        .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "ticket_announcement_open", StringUtil.escapeMarkdown(member.getUser().getAsTag()), textChannel.getAsMention()));

                logChannel.sendMessage(ticketsEntity.getPingStaffRoles() ? getRolePing(ticketsEntity) : " ")
                        .setEmbeds(ebAnnouncement.build())
                        .setComponents(generateButtons(locale, ticketsEntity.getAssignmentMode() == TicketsEntity.AssignmentMode.MANUAL))
                        .setAllowedMentions(Collections.singleton(Message.MentionType.ROLE))
                        .queue(m -> {
                            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(Ticket.class)) {
                                TicketsEntity newticketsEntity = entityManager.findGuildEntity(textChannel.getGuild().getIdLong()).getTickets();
                                entityManager.getTransaction().begin();
                                newticketsEntity.getTicketChannels().put(textChannel.getIdLong(), new TicketChannelEntity(
                                        textChannel.getIdLong(),
                                        member.getIdLong(),
                                        logChannel.getIdLong(),
                                        m.getIdLong(),
                                        false,
                                        finalStarterMessageId,
                                        assignmentMode
                                ));
                                entityManager.getTransaction().commit();
                            }
                            DBStaticReactionMessages.getInstance()
                                    .retrieve(textChannel.getGuild().getIdLong())
                                    .put(m.getIdLong(), new StaticReactionMessageData(m, commandProperties.trigger(), textChannel.getId()));
                        }, e -> {
                            MainLogger.get().error("Ticket announcement error", e);
                            try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(Ticket.class)) {
                                TicketsEntity newticketsEntity = entityManager.findGuildEntity(textChannel.getGuild().getIdLong()).getTickets();
                                entityManager.getTransaction().begin();
                                newticketsEntity.getTicketChannels().put(textChannel.getIdLong(), new TicketChannelEntity(
                                        textChannel.getIdLong(),
                                        member.getIdLong(),
                                        0L,
                                        0L,
                                        false,
                                        finalStarterMessageId,
                                        assignmentMode
                                ));
                                entityManager.getTransaction().commit();
                            }
                        });
            }
        });

        if (announcementNotPosted.get()) {
            ticketsEntity.beginTransaction();
            ticketsEntity.getTicketChannels().put(textChannel.getIdLong(), new TicketChannelEntity(
                    textChannel.getIdLong(),
                    member.getIdLong(),
                    0L,
                    0L,
                    false,
                    finalStarterMessageId,
                    assignmentMode
            ));
            ticketsEntity.commitTransaction();
        }
    }

    private static ActionRow generateButtons(Locale locale, boolean includeAssignButton) {
        List<Button> buttonList = new ArrayList<>();

        if (includeAssignButton) {
            buttonList.add(Button.of(ButtonStyle.PRIMARY, TicketCommand.BUTTON_ID_ASSIGN, TextManager.getString(locale, Category.CONFIGURATION, "ticket_button_assign")));
        }
        buttonList.add(Button.of(ButtonStyle.DANGER, TicketCommand.BUTTON_ID_CLOSE, TextManager.getString(locale, Category.CONFIGURATION, "ticket_button_close")));

        return ActionRow.of(buttonList);
    }

    private static String getRolePing(TicketsEntity ticketsEntity) {
        StringBuilder pings = new StringBuilder();
        ticketsEntity.getStaffRoles()
                .forEach(role -> pings.append(role.getAsMention(ticketsEntity.getHibernateEntity().getLocale())).append(" "));

        if (pings.isEmpty()) {
            return " ";
        }
        return pings.toString();
    }

    private static TextChannel createTicketChannel(TicketsEntity ticketsEntity, TextChannel textChannel, Member member, Locale locale) {
        Guild guild = textChannel.getGuild();
        if (PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, guild, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL) &&
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, textChannel.getParentCategory(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MANAGE_CHANNEL)
        ) {
            int index = ticketsEntity.getTicketIndex();
            index++;
            if (index > 9999) {
                index = 1;
            }
            ticketsEntity.beginTransaction();
            ticketsEntity.setTicketIndex(index);
            ticketsEntity.commitTransaction();

            String ticket = String.format("%04d", index);
            return createNewChannel(ticketsEntity, textChannel, member, ticket).complete();
        }

        return null;
    }

    public static void closeTicket(GuildEntity guildEntity, TicketChannelEntity ticketChannelEntity, TextChannel channel) {
        Locale locale = guildEntity.getLocale();
        TicketsEntity ticketsEntity = guildEntity.getTickets();
        AuditableRestAction<Void> channelDeleteRestAction = channel.delete()
                .reason(Command.getCommandLanguage(TicketCommand.class, locale).getTitle());

        if (!ticketsEntity.getProtocolsEffectively()) {
            if (ticketsEntity.getDeleteChannelsOnClose()) {
                channelDeleteRestAction.queue();
            } else {
                closeChannelWithoutDeletion(locale, guildEntity, ticketChannelEntity, channel);
            }
            return;
        }

        FeatureLogger.inc(PremiumFeature.TICKETS_LOGS, guildEntity.getGuildId());
        try (Lock lock = new Lock(TicketCommand.class)) {
            MessageHistory messageHistory = channel.getHistory();
            List<Message> messageLoadList;
            do {
                messageLoadList = messageHistory.retrievePast(100).complete();
            } while (messageLoadList.size() == 100);

            ArrayList<Message> messageList = new ArrayList<>(messageHistory.getRetrievedHistory());
            Collections.reverse(messageList);

            ArrayList<String[]> csvRows = new ArrayList<>();
            csvRows.add(TextManager.getString(locale, Category.CONFIGURATION, "ticket_csv_titles").split("\n"));
            DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withLocale(Locale.US);
            long lastAuthorId = 0L;
            Instant lastMessageTime = null;

            for (Message message : messageList) {
                String contentRaw = extractContentFromMessage(locale, message);
                if (contentRaw.isEmpty() && message.getAttachments().isEmpty()) {
                    continue;
                }

                String content = WordUtils.wrap(contentRaw, 100);
                String[] row = new String[]{" ", " ", !content.isEmpty() ? content : " ", " "};

                if (message.getAuthor().getIdLong() != lastAuthorId ||
                        lastMessageTime == null ||
                        message.getTimeCreated().toInstant().isAfter(lastMessageTime.plus(Duration.ofMinutes(15)))
                ) {
                    row[0] = formatter.format(message.getTimeCreated());
                    row[1] = message.getAuthor().getAsTag();
                }

                if (!message.getAttachments().isEmpty()) {
                    StringBuilder attachments = new StringBuilder();
                    for (Message.Attachment attachment : message.getAttachments()) {
                        if (!attachments.isEmpty()) {
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

            LocalFile tempFile = new LocalFile(LocalFile.Directory.CDN, String.format("tickets/%s.csv", RandomUtil.generateRandomString(30)));
            try (InputStream is = CSVGenerator.generateInputStream(csvRows)) {
                FileUtil.writeInputStreamToFile(is, tempFile);
            } catch (IOException e) {
                MainLogger.get().error("Error", e);
            }
            TicketProtocolCache.setUrl(channel.getIdLong(), tempFile.cdnGetUrl());

            if (ticketsEntity.getDeleteChannelsOnClose()) {
                channelDeleteRestAction.queue();
            } else {
                closeChannelWithoutDeletion(locale, guildEntity, ticketChannelEntity, channel);
            }
        } catch (LockOccupiedException e) {
            //Ignore
        }
    }

    public static void removeTicket(TextChannel ticketTextChannel, GuildEntity guildEntity, TicketChannelEntity ticketChannelEntity) {
        guildEntity.beginTransaction();
        guildEntity.getTickets().getTicketChannels().remove(ticketChannelEntity.getChannelId());
        guildEntity.commitTransaction();

        TextChannel announcementChannel = ticketTextChannel.getGuild().getTextChannelById(ticketChannelEntity.getLogChannelId());
        if (announcementChannel == null) {
            return;
        }

        Class<TicketCommand> clazz = TicketCommand.class;
        String csvUrl = TicketProtocolCache.getUrl(ticketChannelEntity.getChannelId());
        Locale locale = guildEntity.getLocale();
        String title = Command.getCommandProperties(clazz).emoji() + " " + Command.getCommandLanguage(clazz, locale).getTitle();
        String desc = TextManager.getString(locale, Category.CONFIGURATION, "ticket_announcement_closed",
                StringUtil.escapeMarkdownInField(ticketTextChannel.getName()),
                StringUtil.escapeMarkdown(AtomicUser.fromOutsideCache(ticketChannelEntity.getMemberId()).getTaggedName(locale))
        );

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(title)
                .setDescription(desc);
        if (csvUrl != null) {
            EmbedUtil.addLog(eb, LogStatus.WARNING, TextManager.getString(locale, Category.CONFIGURATION, "ticket_csv_warning"));
        }

        DBStaticReactionMessages.getInstance().retrieve(ticketTextChannel.getGuild().getIdLong())
                .remove(ticketChannelEntity.getLogMessageId());
        MessageEditAction messageAction = announcementChannel.editMessageById(ticketChannelEntity.getLogMessageId(), Emojis.ZERO_WIDTH_SPACE.getFormatted())
                .setComponents()
                .setEmbeds(eb.build());
        if (csvUrl != null) {
            Button button = Button.of(ButtonStyle.LINK, csvUrl, TextManager.getString(locale, Category.CONFIGURATION, "ticket_csv_download"));
            messageAction = messageAction.setComponents(ActionRows.of(button));
        }
        messageAction.queue();
    }

    public static synchronized void assignTicket(Member member, TextChannel channel, GuildEntity guildEntity,
                                                 TicketChannelEntity ticketChannelEntity
    ) throws ExecutionException, InterruptedException {
        Guild guild = member.getGuild();
        Locale locale = guildEntity.getLocale();
        TicketsEntity ticketsEntity = guildEntity.getTickets();

        PermissionOverride memberPermissionOverride = channel.getPermissionOverride(member);
        if (memberPermissionOverride != null && memberPermissionOverride.getAllowed().contains(Permission.MESSAGE_SEND)) {
            return;
        }

        if (ticketChannelEntity.getMemberId() != member.getIdLong() &&
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel.getParentCategory(), Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND) &&
                PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel, Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS)
        ) {
            guildEntity.beginTransaction();
            ticketChannelEntity.setAssigned(true);
            guildEntity.commitTransaction();

            TextChannelManager channelManager = channel.getManager();
            for (Role staffRole : AtomicRole.to(ticketsEntity.getStaffRoles())) {
                PermissionOverride permissionOverride = channel.getPermissionOverride(staffRole);
                if (permissionOverride == null || !permissionOverride.getDenied().contains(Permission.MESSAGE_SEND)) {
                    channelManager = (TextChannelManager) BotPermissionUtil.addPermission(channel, channelManager, staffRole, false, Permission.MESSAGE_SEND);
                }
            }
            BotPermissionUtil.addPermission(channel, channelManager, member, true, Permission.MESSAGE_SEND)
                    .reason(Command.getCommandLanguage(TicketCommand.class, locale).getTitle())
                    .queue();

            HashSet<Long> assignedMemberIds = new HashSet<>(Collections.singleton(member.getIdLong()));
            for (PermissionOverride permissionOverride : channel.getMemberPermissionOverrides()) {
                if (permissionOverride.getAllowed().contains(Permission.MESSAGE_SEND) &&
                        permissionOverride.getIdLong() != ticketChannelEntity.getMemberId() &&
                        permissionOverride.getIdLong() != guild.getSelfMember().getIdLong()
                ) {
                    assignedMemberIds.add(permissionOverride.getIdLong());
                }
            }
            List<User> assignedUsers = MemberCacheController.getInstance().loadMembers(channel.getGuild(), assignedMemberIds).get().stream()
                    .map(Member::getUser)
                    .collect(Collectors.toList());

            TextChannel announcementChannel = guild.getTextChannelById(ticketChannelEntity.getLogChannelId());
            if (announcementChannel != null) {
                String title = Command.getCommandProperties(TicketCommand.class).emoji() + " " + Command.getCommandLanguage(TicketCommand.class, locale).getTitle();
                String desc = TextManager.getString(
                        locale,
                        Category.CONFIGURATION,
                        "ticket_announcement_assigned",
                        channel.getAsMention(),
                        StringUtil.escapeMarkdown(AtomicUser.fromOutsideCache(ticketChannelEntity.getMemberId()).getTaggedName(locale)),
                        MentionUtil.getMentionedStringOfDiscriminatedUsers(locale, assignedUsers).getMentionText()
                );
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(title)
                        .setDescription(desc);
                announcementChannel.editMessageById(ticketChannelEntity.getLogMessageId(), " ")
                        .setEmbeds(eb.build())
                        .queue();
            }

            if (BotPermissionUtil.canWriteEmbed(channel)) {
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "ticket_assign", member.getUser().getAsTag()));
                channel.sendMessageEmbeds(eb.build()).queue();
            }
        }
    }

    private static ChannelAction<TextChannel> createNewChannel(TicketsEntity ticketsEntity, TextChannel parentChannel, Member member, String ticket) {
        ChannelAction<TextChannel> channelAction;
        if (parentChannel.getParentCategory() != null) {
            channelAction = parentChannel.getParentCategory().createTextChannel(ticket);
        } else {
            channelAction = parentChannel.getGuild().createTextChannel(ticket);
        }

        channelAction = BotPermissionUtil.clearPermissionOverrides(channelAction);
        return addPermissions(ticketsEntity, parentChannel, channelAction, AtomicRole.to(ticketsEntity.getStaffRoles()), member);
    }

    private static ChannelAction<TextChannel> addPermissions(TicketsEntity ticketsEntity, TextChannel parentChannel, ChannelAction<TextChannel> channelAction, List<Role> staffRoles, Member member) {
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member.getGuild().getPublicRole(), false, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
        for (PermissionOverride permissionOverride : parentChannel.getPermissionOverrides()) {
            if (permissionOverride.isRoleOverride()) {
                channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, permissionOverride, false, Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND);
            } else {
                channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, permissionOverride, false);
            }
        }

        Permission[] staffRolePermissions = ticketsEntity.getAssignmentMode() != TicketsEntity.AssignmentMode.MANUAL
                ? new Permission[]{Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND}
                : new Permission[]{Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY};
        for (Role staffRole : staffRoles) {
            channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, staffRole, true, staffRolePermissions);
        }

        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, member, true, Permission.VIEW_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND);
        channelAction = BotPermissionUtil.addPermission(parentChannel, channelAction, parentChannel.getGuild().getSelfMember(), true,
                Permission.VIEW_CHANNEL, Permission.MANAGE_CHANNEL, Permission.MESSAGE_HISTORY, Permission.MESSAGE_SEND
        );
        return channelAction;
    }

    private static void closeChannelWithoutDeletion(Locale locale, GuildEntity guildEntity, TicketChannelEntity ticketChannelEntity, TextChannel channel) {
        if (ticketChannelEntity.getIntroductionMessageId() != 0) {
            channel.editMessageComponentsById(ticketChannelEntity.getIntroductionMessageId())
                    .queue();
        }

        TextChannelManager channelManager = channel.getManager()
                .removePermissionOverride(ticketChannelEntity.getMemberId());

        for (PermissionOverride permissionOverride : channel.getPermissionOverrides()) {
            if (permissionOverride.getIdLong() != ticketChannelEntity.getMemberId() &&
                    permissionOverride.getIdLong() != channel.getGuild().getSelfMember().getIdLong() &&
                    permissionOverride.getAllowed().contains(Permission.MESSAGE_SEND)
            ) {
                channelManager = (TextChannelManager) BotPermissionUtil.addPermission(channel, channelManager, permissionOverride, false, Permission.MESSAGE_SEND);
            }
        }

        channelManager.reason(Command.getCommandLanguage(TicketCommand.class, locale).getTitle())
                .queue();

        if (PermissionCheckRuntime.botHasPermission(locale, TicketCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(TextManager.getString(locale, Category.CONFIGURATION, "ticket_close_title"))
                    .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "ticket_close"));
            channel.sendMessageEmbeds(eb.build()).queue();
        }

        Ticket.removeTicket(channel, guildEntity, ticketChannelEntity);
    }

    private static String extractContentFromMessage(Locale locale, Message message) {
        String content = message.getContentDisplay();
        if (!message.getEmbeds().isEmpty() &&
                message.getEmbeds().get(0).getDescription() != null &&
                !message.getEmbeds().get(0).getDescription().isEmpty()
        ) {
            MessageEmbed messageEmbed = message.getEmbeds().get(0);
            String newContent = message.getContentDisplay().isBlank()
                    ? messageEmbed.getDescription()
                    : message.getContentDisplay() + " | " + messageEmbed.getDescription();
            if (messageEmbed.getAuthor() != null &&
                    messageEmbed.getAuthor().getName() != null
            ) {
                content = TextManager.getString(
                        locale,
                        Category.CONFIGURATION,
                        "ticket_csv_author",
                        messageEmbed.getAuthor().getName(),
                        newContent
                );
            } else {
                content = newContent;
            }
        }
        return content;
    }

}
