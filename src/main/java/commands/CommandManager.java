package commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import commands.cooldownchecker.CoolDownManager;
import commands.cooldownchecker.CoolDownUserData;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.informationcategory.PingCommand;
import commands.runningchecker.RunningCheckerManager;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import constants.Settings;
import core.*;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import core.schedule.MainScheduler;
import core.utils.*;
import mysql.modules.commandmanagement.DBCommandManagement;
import mysql.modules.whitelistedchannels.DBWhiteListedChannels;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;

public class CommandManager {

    private final static int SEC_UNTIL_REMOVAL = 20;
    private final static Random random = new Random();

    public static void manage(GuildMessageReceivedEvent event, Command command, String args, Instant startTime) {
        if (checkCoolDown(event, command) &&
                checkRunningCommands(event, command)
        ) {
            process(event, command, args, startTime);
        }

        command.getCompletedListeners().forEach(runnable -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                MainLogger.get().error("Error on completed listener", e);
            }
        });
    }

    private static void process(GuildMessageReceivedEvent event, Command command, String args, Instant startTime) {
        if (botCanPost(event, command) &&
                isWhiteListed(event, command) &&
                botCanUseEmbeds(event, command) &&
                canRunOnServer(event, command) &&
                isNSFWCompliant(event, command) &&
                checkTurnedOn(event, command) &&
                checkPermissions(event, command) &&
                checkPatreon(event, command) &&
                checkReleased(event, command)
        ) {
            if (command.getCommandProperties().patreonRequired() &&
                    (Arrays.stream(command.getCommandProperties().userGuildPermissions()).anyMatch(p -> p == Permission.MANAGE_SERVER))
            ) {
                ServerPatreonBoostCache.getInstance().setTrue(event.getGuild().getIdLong());
            }

            try {
                cleanPreviousListeners(command, event.getMember());
                sendOverwrittenSignals(command, event.getMember());

                if (command instanceof PingCommand) {
                    command.getAttachments().put("starting_time", startTime);
                }

                boolean success = command.processTrigger(event, args);
                if (success && Program.isPublicVersion()) {
                    maybeSendBotInvite(event, command.getLocale());
                }
            } catch (Throwable e) {
                ExceptionUtil.handleCommandException(e, command, event.getChannel());
            } finally {
                CommandContainer.getInstance().cleanUp();
            }
        }
    }

    private static void maybeSendBotInvite(GuildMessageReceivedEvent event, Locale locale) {
        if (random.nextInt(180) == 0 &&
                !BotPermissionUtil.can(event.getMember(), Permission.MANAGE_SERVER) &&
                !BotPermissionUtil.can(event.getMember(), Permission.MESSAGE_MANAGE) &&
                BotPermissionUtil.canWriteEmbed(event.getChannel())
        ) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setThumbnail(ShardManager.getInstance().getSelf().getAvatarUrl())
                    .setDescription(TextManager.getString(locale, TextManager.GENERAL, "invite", ExternalLinks.BOT_INVITE_REMINDER_URL));

            event.getChannel().sendMessage(eb.build()).queue();
        }
    }

    private static boolean checkRunningCommands(GuildMessageReceivedEvent event, Command command) {
        if (command instanceof PingCommand) command.getAttachments().put("start_instant", Instant.now());
        if (RunningCheckerManager.getInstance().canUserRunCommand(
                command,
                event.getGuild().getIdLong(),
                event.getMember().getIdLong(),
                event.getJDA().getShardInfo().getShardId(),
                command.getCommandProperties().maxCalculationTimeSec()
        )) {
            return true;
        }

        if (CoolDownManager.getInstance().getCoolDownData(event.getMember().getIdLong()).canPostCoolDownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_desc");

            if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "alreadyused_title"))
                        .setDescription(desc);
                sendError(event, command.getLocale(), eb, true);
            } else if (BotPermissionUtil.canWrite(event.getChannel())) {
                sendErrorNoEmbed(event, command.getLocale(), desc, true);
            }
        }

        return false;
    }

    private static boolean checkCoolDown(GuildMessageReceivedEvent event, Command command) {
        if (PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) >= 3 || PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())) {
            return true;
        }
        CoolDownUserData cooldownUserData = CoolDownManager.getInstance().getCoolDownData(event.getMember().getIdLong());

        Optional<Integer> waitingSec = cooldownUserData.getWaitingSec(Settings.COOLDOWN_TIME_SEC);
        if (waitingSec.isEmpty()) {
            return true;
        }

        if (cooldownUserData.canPostCoolDownMessage()) {
            String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_description", waitingSec.get() != 1, String.valueOf(waitingSec.get()));

            if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
                EmbedBuilder eb = EmbedFactory.getEmbedError()
                        .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "cooldown_title"))
                        .setDescription(desc);
                sendError(event, command.getLocale(), eb, false);
            } else if (BotPermissionUtil.canWrite(event.getChannel())) {
                sendErrorNoEmbed(event, command.getLocale(), desc, false);
            }
        }

        return false;
    }

    private static boolean checkReleased(GuildMessageReceivedEvent event, Command command) {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (!releaseDate.isAfter(LocalDate.now()) ||
                PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) > 1 ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description", ExternalLinks.PATREON_PAGE, ExternalLinks.UNLOCK_SERVER_WEBSITE);
        String waitTime = TextManager.getString(command.getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(command.getLocale(), Instant.now(), TimeUtil.localDateToInstant(releaseDate), false));

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            EmbedUtil.addLog(eb, LogStatus.TIME, waitTime);
            sendError(event, command.getLocale(), eb, false);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc + "\n\n`" + waitTime + "`", false);
        }

        return false;
    }

    private static boolean checkPatreon(GuildMessageReceivedEvent event, Command command) {
        if (!command.getCommandProperties().patreonRequired() ||
                PatreonCache.getInstance().getUserTier(event.getMember().getIdLong(), true) > 1 ||
                PatreonCache.getInstance().isUnlocked(event.getGuild().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(
                command.getLocale(),
                TextManager.GENERAL,
                "patreon_description",
                ExternalLinks.PATREON_PAGE,
                ExternalLinks.UNLOCK_SERVER_WEBSITE
        );

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb, false);
        } else if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc, false);
        }

        return false;
    }

    private static boolean checkPermissions(GuildMessageReceivedEvent event, Command command) {
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                command.getLocale(),
                event.getChannel(),
                event.getMember(),
                command.getAdjustedUserGuildPermissions(),
                command.getAdjustedUserChannelPermissions(),
                command.getAdjustedBotGuildPermissions(),
                command.getAdjustedBotChannelPermissions()
        );
        if (errEmbed == null) {
            return true;
        }

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            sendError(event, command.getLocale(), errEmbed, true);
        }
        return false;
    }

    private static boolean checkTurnedOn(GuildMessageReceivedEvent event, Command command) {
        if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR) ||
                DBCommandManagement.getInstance().retrieve(event.getGuild().getIdLong()).commandIsTurnedOn(command)
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_description");

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "turnedoff_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb, true);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc, true);
        }
        return false;
    }

    private static boolean canRunOnServer(GuildMessageReceivedEvent event, Command command) {
        return command.canRunOnGuild(event.getGuild().getIdLong(), event.getMember().getIdLong());
    }

    private static boolean botCanUseEmbeds(GuildMessageReceivedEvent event, Command command) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel()) || !command.getCommandProperties().requiresEmbeds()) {
            return true;
        }

        sendErrorNoEmbed(event, command.getLocale(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_embed"), true);
        sendHelpDm(event.getMember(), command);
        return false;
    }

    private static boolean isNSFWCompliant(GuildMessageReceivedEvent event, Command command) {
        if (!command.getCommandProperties().nsfw() || event.getChannel().isNSFW()) {
            return true;
        }

        EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale());
        sendError(event, command.getLocale(), eb, true);
        return false;
    }

    private static void sendErrorNoEmbed(GuildMessageReceivedEvent event, Locale locale, String text, boolean autoDelete) {
        if (BotPermissionUtil.canWrite(event.getChannel())) {
            MessageAction messageAction;
            if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_HISTORY)) {
                messageAction = event.getMessage()
                        .reply(TextManager.getString(locale, TextManager.GENERAL, "command_block", text));
            } else {
                messageAction = event.getChannel()
                        .sendMessage(TextManager.getString(locale, TextManager.GENERAL, "command_block", text));
            }

            if (autoDelete) {
                messageAction.queue(message -> autoRemoveMessageAfterCountdown(event, message));
            } else {
                messageAction.queue();
            }
        }
    }

    private static void sendError(GuildMessageReceivedEvent event, Locale locale, EmbedBuilder eb, boolean autoDelete) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            if (autoDelete) {
                eb.setFooter(TextManager.getString(locale, TextManager.GENERAL, "deleteTime", String.valueOf(SEC_UNTIL_REMOVAL)));
            }

            MessageAction messageAction;
            if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_HISTORY)) {
                messageAction = event.getMessage()
                        .reply(eb.build());
            } else {
                messageAction = event.getChannel()
                        .sendMessage(eb.build());
            }

            if (autoDelete) {
                messageAction.queue(message -> autoRemoveMessageAfterCountdown(event, message));
            } else {
                messageAction.queue();
            }
        }
    }

    private static void autoRemoveMessageAfterCountdown(GuildMessageReceivedEvent event, Message message) {
        MainScheduler.getInstance().schedule(SEC_UNTIL_REMOVAL, ChronoUnit.SECONDS, "command_manager_error_countdown", () -> {
            if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                event.getChannel().deleteMessages(Arrays.asList(message, event.getMessage())).queue();
            } else {
                message.delete().queue();
            }
        });
    }

    private static boolean isWhiteListed(GuildMessageReceivedEvent event, Command command) {
        if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR) ||
                DBWhiteListedChannels.getInstance().retrieve(event.getGuild().getIdLong()).isWhiteListed(event.getChannel().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_description");

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb, true);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc, true);
        }
        return false;
    }

    private static boolean botCanPost(GuildMessageReceivedEvent event, Command command) {
        if (BotPermissionUtil.canWrite(event.getChannel())) {
            return true;
        }

        if (BotPermissionUtil.canReadHistory(event.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
            RestActionQueue restActionQueue = new RestActionQueue();
            restActionQueue.attach(event.getMessage().addReaction(Emojis.X));
            restActionQueue.attach(event.getMessage().addReaction("✍️"))
                    .getCurrentRestAction()
                    .queue();
        }

        if (!sendHelpDm(event.getMember(), command)) {
            if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR)) {
                JDAUtil.sendPrivateMessage(
                        event.getMember(),
                        TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_writing_permissions", StringUtil.escapeMarkdown(event.getChannel().getName()))
                ).queue();
            }
        }

        return false;
    }

    private static boolean sendHelpDm(Member member, Command command) {
        if (command instanceof HelpCommand) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault(command, command.getString("dm", ExternalLinks.COMMANDS_WEBSITE))
                    .setTitle(null);
            EmbedUtil.setMemberAuthor(eb, member.getGuild().getSelfMember());
            JDAUtil.sendPrivateMessage(member, eb.build()).queue();
            return true;
        }
        return false;
    }

    private static void sendOverwrittenSignals(Command command, Member member) {
        sendOverwrittenSignals(command, member, OnReactionListener.class);
        sendOverwrittenSignals(command, member, OnMessageInputListener.class);
    }

    private static void sendOverwrittenSignals(Command command, Member member, Class<?> clazz) {
        if (clazz.isInstance(command)) {
            CommandContainer.getInstance().getListeners(clazz).stream()
                    .filter(meta -> meta.getAuthorId() == member.getIdLong())
                    .forEach(CommandListenerMeta::override);
        }
    }

    private static void cleanPreviousListeners(Command command, Member member) {
        cleanPreviousListeners(command, member, OnReactionListener.class);
        cleanPreviousListeners(command, member, OnMessageInputListener.class);
    }

    private static void cleanPreviousListeners(Command command, Member member, Class<?> clazz) {
        if (clazz.isInstance(command)) {
            ArrayList<CommandListenerMeta<?>> metaList = CommandContainer.getInstance().getListeners(clazz).stream()
                    .filter(meta -> meta.getAuthorId() == member.getIdLong())
                    .sorted(Comparator.comparing(CommandListenerMeta::getCreationTime))
                    .collect(Collectors.toCollection(ArrayList::new));

            while (metaList.size() >= 2) {
                CommandListenerMeta<?> meta = metaList.remove(0);
                CommandContainer.getInstance().deregisterListener(clazz, meta.getCommand());
                meta.timeOut();
            }
        }
    }

    public static Optional<Command> createCommandByTrigger(String trigger, Locale locale, String prefix) {
        Class<? extends Command> clazz = CommandContainer.getInstance().getCommandMap().get(trigger);
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

}
