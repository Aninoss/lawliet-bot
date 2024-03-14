package commands;

import commands.cooldownchecker.CoolDownManager;
import commands.cooldownchecker.CoolDownUserData;
import commands.listeners.*;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.informationcategory.PingCommand;
import commands.runningchecker.RunningCheckerManager;
import constants.*;
import core.*;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import core.components.ActionRows;
import core.schedule.MainScheduler;
import core.utils.*;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.whitelistedchannels.DBWhiteListedChannels;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {

    private final static int SEC_UNTIL_REMOVAL = 60;
    private final static Random random = new Random();

    public static void manage(CommandEvent event, Command command, String args, GuildEntity guildEntity, Instant startTime) {
        manage(event, command, args, guildEntity, startTime, true);
    }

    public static void manage(CommandEvent event, Command command, String args, GuildEntity guildEntity, Instant startTime, boolean freshCommand) {
        if (command instanceof PingCommand) {
            command.getAttachments().put("starting_time", startTime);
        }

        if (checkCoolDown(event, guildEntity, command) &&
                checkCorrectChannelType(event, guildEntity, command) &&
                checkRunningCommands(event, guildEntity, command)
        ) {
            process(event, command, args, guildEntity, freshCommand);
        }

        command.getCompletedListeners().forEach(runnable -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                MainLogger.get().error("Error on completed listener", e);
            }
        });
    }

    private static void process(CommandEvent event, Command command, String args, GuildEntity guildEntity, boolean freshCommand) {
        if (botCanPost(event, command) &&
                isWhiteListed(event, guildEntity, command) &&
                botCanUseEmbeds(event, guildEntity, command) &&
                canRunOnGuild(event, command) &&
                isNSFWCompliant(event, guildEntity, command) &&
                checkTurnedOn(event, guildEntity, command) &&
                checkCommandPermissions(event, guildEntity, command) &&
                checkPermissions(event, guildEntity, command) &&
                checkPatreon(event, guildEntity, command) &&
                checkReleased(event, guildEntity, command) &&
                checkArgsProvided(event, guildEntity, command, args)
        ) {
            if (command.getCommandProperties().patreonRequired() &&
                    (Arrays.stream(command.getCommandProperties().userGuildPermissions()).anyMatch(p -> p == Permission.MANAGE_SERVER))
            ) {
                ServerPatreonBoostCache.setTrue(event.getGuild().getIdLong());
            }

            try {
                cleanPreviousListeners(command, event.getMember());
                sendOverwrittenSignals(command, event.getMember());

                boolean success = command.processTrigger(event, args, guildEntity, freshCommand);
                if (success && Program.publicInstance()) {
                    maybeSendBotInvite(event, command.getLocale());
                }
            } catch (Throwable e) {
                ExceptionUtil.handleCommandException(e, command, event, guildEntity);
            } finally {
                CommandContainer.cleanUp();
            }
        }
    }

    private static void maybeSendBotInvite(CommandEvent event, Locale locale) {
        if (Program.publicInstance() &&
                random.nextInt(180) == 0 &&
                !BotPermissionUtil.can(event.getMember(), Permission.MANAGE_SERVER, Permission.MESSAGE_MANAGE) &&
                BotPermissionUtil.canWriteEmbed(event.getTextChannel())
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setThumbnail(ShardManager.getSelf().getAvatarUrl())
                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "invite"));

            Button button = Button.of(ButtonStyle.LINK, ExternalLinks.BOT_INVITE_REMINDER_URL, TextManager.getString(locale, TextManager.GENERAL, "invite_button"));
            event.getTextChannel().sendMessageEmbeds(eb.build())
                    .setComponents(ActionRows.of(button))
                    .queue();
        }
    }

    private static boolean checkRunningCommands(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (RunningCheckerManager.canUserRunCommand(
                command,
                event.getGuild().getIdLong(),
                event.getMember().getIdLong(),
                event.getJDA().getShardInfo().getShardId(),
                command.getCommandProperties().maxCalculationTimeSec()
        )) {
            return true;
        }

        if (CoolDownManager.getCoolDownData(event.getMember().getIdLong()).canPostCoolDownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_desc");

            if (BotPermissionUtil.canWriteEmbed(event.getGuildMessageChannel()) || event.isSlashCommandInteractionEvent()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_title"))
                        .setDescription(desc);
                sendError(event, guildEntity, eb, true);
            } else if (BotPermissionUtil.canWrite(event.getGuildMessageChannel())) {
                sendErrorNoEmbed(event, guildEntity, desc, true);
            }
        }

        return false;
    }

    private static boolean checkCorrectChannelType(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (event.getChannel() instanceof TextChannel) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "wrongchanneltype_desc");
        if (BotPermissionUtil.canWriteEmbed(event.getGuildMessageChannel()) || event.isSlashCommandInteractionEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "wrongchanneltype_title"))
                    .setDescription(desc);
            sendError(event, guildEntity, eb, true);
        } else if (BotPermissionUtil.canWrite(event.getGuildMessageChannel())) {
            sendErrorNoEmbed(event, guildEntity, desc, true);
        }

        return false;
    }

    private static boolean checkCoolDown(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) || PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())) {
            return true;
        }
        CoolDownUserData cooldownUserData = CoolDownManager.getCoolDownData(event.getMember().getIdLong());

        Optional<Integer> waitingSec = cooldownUserData.getWaitingSec(Settings.COOLDOWN_TIME_SEC);
        if (waitingSec.isEmpty()) {
            return true;
        }

        if (cooldownUserData.canPostCoolDownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_description", waitingSec.get() != 1, String.valueOf(waitingSec.get()));

            if (BotPermissionUtil.canWriteEmbed(event.getGuildMessageChannel()) || event.isSlashCommandInteractionEvent()) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_title"))
                        .setDescription(desc);
                sendError(event, guildEntity, eb, false);
            } else if (BotPermissionUtil.canWrite(event.getGuildMessageChannel())) {
                sendErrorNoEmbed(event, guildEntity, desc, false);
            }
        }

        return false;
    }

    private static boolean checkArgsProvided(CommandEvent event, GuildEntity guildEntity, Command command, String args) {
        if (command.getCommandProperties().executableWithoutArgs() || !args.isEmpty()) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_args");
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel()) || event.isSlashCommandInteractionEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "wrong_args"))
                    .setDescription(desc);
            sendError(event, guildEntity, eb, true);
        } else if (BotPermissionUtil.canWrite(event.getTextChannel())) {
            sendErrorNoEmbed(event, guildEntity, desc, true);
        }
        return false;
    }

    private static boolean checkReleased(CommandEvent event, GuildEntity guildEntity, Command command) {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (!releaseDate.isAfter(LocalDate.now()) ||
                PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description");
        String waitTime = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_releaseday", TimeFormat.DATE_TIME_SHORT.atInstant(TimeUtil.localDateToInstant(releaseDate)).toString());

        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel()) || event.isSlashCommandInteractionEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PREMIUM_COLOR)
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"))
                    .setDescription(desc);
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), waitTime, false);
            sendError(event, guildEntity, eb, false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        } else if (BotPermissionUtil.canWrite(event.getTextChannel())) {
            sendErrorNoEmbed(event, guildEntity, desc + "\n\n" + waitTime, false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        }

        return false;
    }

    private static boolean checkPatreon(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (!command.getCommandProperties().patreonRequired() ||
                PatreonCache.getInstance().hasPremium(event.getMember().getIdLong(), true) ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())
        ) {
            return true;
        }

        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel()) || event.isSlashCommandInteractionEvent()) {
            sendError(event, guildEntity, EmbedFactory.getPatreonBlockEmbed(command.getLocale()), false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        } else if (BotPermissionUtil.canWrite(event.getTextChannel())) {
            sendErrorNoEmbed(event, guildEntity, TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_description_noembed"), false, EmbedFactory.getPatreonBlockButtons(command.getLocale()));
        }

        return false;
    }

    private static boolean checkPermissions(CommandEvent event, GuildEntity guildEntity, Command command) {
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                command.getLocale(),
                event.getTextChannel(),
                event.getMember(),
                command.getAdjustedUserGuildPermissions(),
                command.getAdjustedUserChannelPermissions(),
                command.getAdjustedBotGuildPermissions(),
                command.getAdjustedBotChannelPermissions()
        );
        if (errEmbed == null) {
            return true;
        }

        sendError(event, guildEntity, errEmbed, true);
        return false;
    }

    private static boolean checkCommandPermissions(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (CommandPermissions.hasAccess(command.getClass(), event.getMember(), event.getTextChannel(), false)) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "permissionsblock_description", command.getPrefix());
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel()) || event.isSlashCommandInteractionEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "permissionsblock_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, guildEntity, eb, true);
        } else if (BotPermissionUtil.canWrite(event.getTextChannel())) {
            sendErrorNoEmbed(event, guildEntity, desc, true);
        }
        return false;
    }

    private static boolean checkTurnedOn(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (DisabledCommands.commandIsEnabledEffectively(guildEntity, command, event.getMember())) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_description");
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel()) || event.isSlashCommandInteractionEvent()) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, guildEntity, eb, true);
        } else if (BotPermissionUtil.canWrite(event.getTextChannel())) {
            sendErrorNoEmbed(event, guildEntity, desc, true);
        }
        return false;
    }

    private static boolean canRunOnGuild(CommandEvent event, Command command) {
        return command.canRunOnGuild(event.getGuild().getIdLong(), event.getMember().getIdLong());
    }

    private static boolean botCanUseEmbeds(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel()) || !command.getCommandProperties().requiresEmbeds() || event.isSlashCommandInteractionEvent()) {
            return true;
        }

        sendErrorNoEmbed(event, guildEntity, TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_embed"), true);
        sendHelpDm(event.getMember(), command);
        return false;
    }

    private static boolean isNSFWCompliant(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (!command.getCommandProperties().nsfw() || event.getTextChannel().isNSFW()) {
            return true;
        }

        EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale(), command.getPrefix());
        sendError(event, guildEntity, eb, true);
        return false;
    }

    private static void sendErrorNoEmbed(CommandEvent event, GuildEntity guildEntity, String text, boolean autoDelete, Button... buttons) {
        if (BotPermissionUtil.canWrite(event.getGuildMessageChannel()) || event.isSlashCommandInteractionEvent()) {
            RestAction<Message> messageAction = event.replyMessage(guildEntity, TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "command_block", text))
                    .setComponents(ActionRows.of(buttons));
            if (autoDelete) {
                messageAction.queue(message -> autoRemoveMessageAfterCountdown(event, message));
            } else {
                messageAction.queue();
            }
        }
    }

    private static void sendError(CommandEvent event, GuildEntity guildEntity, EmbedBuilder eb, boolean autoDelete, Button... buttons) {
        if (BotPermissionUtil.canWriteEmbed(event.getGuildMessageChannel()) || event.isSlashCommandInteractionEvent()) {
            if (autoDelete) {
                eb.setFooter(TextManager.getString(guildEntity.getLocale(), TextManager.GENERAL, "deleteTime", String.valueOf(SEC_UNTIL_REMOVAL)));
            }

            RestAction<Message> messageAction = event.replyMessageEmbeds(guildEntity, List.of(eb.build()))
                    .setComponents(ActionRows.of(buttons));
            if (autoDelete) {
                messageAction.queue(message -> autoRemoveMessageAfterCountdown(event, message));
            } else {
                messageAction.queue();
            }
        }
    }

    private static void autoRemoveMessageAfterCountdown(CommandEvent event, Message message) {
        MainScheduler.schedule(Duration.ofSeconds(SEC_UNTIL_REMOVAL), () -> {
            if (BotPermissionUtil.can(event.getGuildMessageChannel())) {
                ArrayList<Message> messageList = new ArrayList<>();
                if (message != null) {
                    messageList.add(message);
                }
                if (event.isMessageReceivedEvent() && BotPermissionUtil.can(event.getGuildMessageChannel(), Permission.MESSAGE_MANAGE)) {
                    messageList.add(event.getMessageReceivedEvent().getMessage());
                }
                if (messageList.size() >= 2) {
                    event.getGuildMessageChannel().deleteMessages(messageList).submit()
                            .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL, ExceptionIds.MISSING_ACCESS));
                } else if (messageList.size() >= 1) {
                    event.getGuildMessageChannel().deleteMessageById(messageList.get(0).getId()).submit()
                            .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL, ExceptionIds.MISSING_ACCESS));
                }
            }
        });
    }

    private static boolean isWhiteListed(CommandEvent event, GuildEntity guildEntity, Command command) {
        if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR) ||
                DBWhiteListedChannels.getInstance().retrieve(event.getGuild().getIdLong()).isWhiteListed(event.getTextChannel().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_description");

        if (BotPermissionUtil.canWriteEmbed(event.getTextChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, guildEntity, eb, true);
        } else if (BotPermissionUtil.canWrite(event.getTextChannel())) {
            sendErrorNoEmbed(event, guildEntity, desc, true);
        }
        return false;
    }

    private static boolean botCanPost(CommandEvent event, Command command) {
        if (BotPermissionUtil.canWrite(event.getTextChannel()) || event.isSlashCommandInteractionEvent()) {
            return true;
        }

        if (event.isMessageReceivedEvent() &&
                BotPermissionUtil.canReadHistory(event.getTextChannel(), Permission.MESSAGE_ADD_REACTION)
        ) {
            Message message = event.getMessageReceivedEvent().getMessage();
            RestActionQueue restActionQueue = new RestActionQueue();
            restActionQueue.attach(message.addReaction(Emojis.X));
            restActionQueue.attach(message.addReaction(Emoji.fromUnicode("✍️")))
                    .getCurrentRestAction()
                    .queue();
        }

        if (!sendHelpDm(event.getMember(), command)) {
            if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR)) {
                String text = TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_writing_permissions", StringUtil.escapeMarkdown(event.getTextChannel().getName()));
                JDAUtil.openPrivateChannel(event.getMember())
                        .flatMap(messageChannel -> messageChannel.sendMessage(text))
                        .queue();
            }
        }

        return false;
    }

    private static boolean sendHelpDm(Member member, Command command) {
        if (command instanceof HelpCommand) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, command.getString("dm", ExternalLinks.COMMANDS_WEBSITE))
                    .setTitle(null);
            Member lawliet = member.getGuild().getSelfMember();
            EmbedUtil.setMemberAuthor(eb, lawliet);
            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessageEmbeds(eb.build()))
                    .queue();
            return true;
        }
        return false;
    }

    private static void sendOverwrittenSignals(Command command, Member member) {
        sendOverwrittenSignals(command, member, OnReactionListener.class);
        sendOverwrittenSignals(command, member, OnMessageInputListener.class);
        sendOverwrittenSignals(command, member, OnButtonListener.class);
        sendOverwrittenSignals(command, member, OnStringSelectMenuListener.class);
        sendOverwrittenSignals(command, member, OnEntitySelectMenuListener.class);
    }

    private static void sendOverwrittenSignals(Command command, Member member, Class<?> clazz) {
        if (clazz.isInstance(command)) {
            CommandContainer.getListeners(clazz).stream()
                    .filter(meta -> meta.getAuthorId() == member.getIdLong() && meta.getCommand().getCommandProperties().enableCacheWipe())
                    .forEach(CommandListenerMeta::override);
        }
    }

    private static void cleanPreviousListeners(Command command, Member member) {
        for (CommandContainer.ListenerKey listenerKey : CommandContainer.getListenerKeys()) {
            if (!listenerKey.isWithCacheWipe()) {
                continue;
            }

            Class<?> clazz = listenerKey.getClazz();
            if (!clazz.isInstance(command)) {
                continue;
            }

            ArrayList<CommandListenerMeta<?>> metaList = CommandContainer.getListeners(clazz).stream()
                    .filter(meta -> meta.getAuthorId() == member.getIdLong())
                    .sorted(Comparator.comparing(CommandListenerMeta::getCreationTime))
                    .collect(Collectors.toCollection(ArrayList::new));

            while (metaList.size() >= 2) {
                CommandListenerMeta<?> meta = metaList.remove(0);
                CommandContainer.deregisterListeners(meta.getCommand());
                meta.timeOut();
            }
        }
    }

    public static Optional<Command> createCommandByTrigger(String trigger, Locale locale, String prefix) {
        Class<? extends Command> clazz = CommandContainer.getCommandMap().get(trigger);
        if (clazz == null) return Optional.empty();
        return Optional.of(createCommandByClass(clazz, locale, prefix));
    }

    public static Command createCommandByClassName(String className, Locale locale, String prefix) {
        try {
            return createCommandByClass((Class<? extends Command>) Class.forName(className), locale, prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Command createCommandByClass(Class<? extends Command> clazz, Locale locale, String prefix) {
        for (Constructor<?> s : clazz.getConstructors()) {
            if (s.getParameterCount() == 2) {
                try {
                    return (Command) s.newInstance(locale, prefix);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new RuntimeException("Invalid class");
    }

    public static boolean commandIsEnabledIgnoreAdmin(GuildEntity guildEntity, Command command, Member member, TextChannel channel) {
        return CommandPermissions.hasAccess(command.getClass(), member, channel, true) &&
                DisabledCommands.commandIsEnabled(guildEntity, command);
    }

    public static boolean commandIsEnabledEffectively(GuildEntity guildEntity, Command command, Member member, TextChannel channel) {
        return CommandPermissions.hasAccess(command.getClass(), member, channel, false) &&
                DisabledCommands.commandIsEnabledEffectively(guildEntity, command, member);
    }

    public static boolean commandIsEnabledIgnoreAdmin(GuildEntity guildEntity, Class<? extends Command> clazz, Member member, TextChannel channel) {
        return CommandPermissions.hasAccess(clazz, member, channel, true) &&
                DisabledCommands.elementIsEnabled(guildEntity, Command.getCommandProperties(clazz).trigger()) &&
                DisabledCommands.elementIsEnabled(guildEntity, Command.getCategory(clazz).getId());
    }

    public static boolean commandIsEnabledEffectively(GuildEntity guildEntity, Class<? extends Command> clazz, Member member, TextChannel channel) {
        return CommandPermissions.hasAccess(clazz, member, channel, false) &&
                DisabledCommands.elementIsEnabledEffectively(guildEntity, Command.getCommandProperties(clazz).trigger(), member) &&
                DisabledCommands.elementIsEnabledEffectively(guildEntity, Command.getCategory(clazz).getId(), member);
    }

    public static boolean commandCategoryIsEnabledIgnoreAdmin(GuildEntity guildEntity, Category category, Member member, TextChannel channel) {
        return CommandPermissions.hasAccess(category, member, channel, true) &&
                DisabledCommands.commandCategoryIsEnabled(guildEntity, category);
    }

    public static boolean commandCategoryIsEnabledEffectively(GuildEntity guildEntity, Category category, Member member, TextChannel channel) {
        return CommandPermissions.hasAccess(category, member, channel, false) &&
                DisabledCommands.commandCategoryIsEnabledEffectively(guildEntity, category, member);
    }

}
