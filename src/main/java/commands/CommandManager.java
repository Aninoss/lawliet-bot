package commands;

import commands.cooldownchecker.CoolDownManager;
import commands.cooldownchecker.CoolDownUserData;
import commands.listeners.OnMessageInputListener;
import commands.listeners.OnReactionListener;
import commands.runnables.informationcategory.HelpCommand;
import commands.runnables.informationcategory.PingCommand;
import commands.runningchecker.RunningCheckerManager;
import constants.*;
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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class CommandManager {

    private final static int SEC_UNTIL_REMOVAL = 20;

    public static void manage(GuildMessageReceivedEvent event, Command command, String args, Instant startTime) throws IOException, ExecutionException, InterruptedException, SQLException {
        if (botCanPost(event, command) &&
                isWhiteListed(event, command) &&
                checkCoolDown(event, command) &&
                botCanUseEmbeds(event, command) &&
                canRunOnServer(event, command) &&
                isNSFWCompliant(event, command) &&
                checkTurnedOn(event, command) &&
                checkPermissions(event, command) &&
                checkPatreon(event, command) &&
                checkReleased(event, command) &&
                checkRunningCommands(event, command)
        ) {
            if (command.getCommandProperties().patreonRequired() &&
                    (Arrays.stream(command.getCommandProperties().userGuildPermissions()).anyMatch(p -> p == Permission.MANAGE_SERVER))
            ) {
                ServerPatreonBoostCache.getInstance().setTrue(event.getGuild().getIdLong());
            }

            try {
                cleanPreviousListeners(command, event.getMember());
                sendOverwrittenSignals(command, event.getMember());

                if (command instanceof PingCommand)
                    command.getAttachments().put("starting_time", startTime);

                command.onTrigger(event, args);
                if (Bot.isPublicVersion())
                    maybeSendInvite(event, command.getLocale());
            } catch (Throwable e) {
                ExceptionUtil.handleCommandException(e, command, event.getChannel());
            }
        }
    }

    private static void maybeSendInvite(GuildMessageReceivedEvent event, Locale locale) {
        if (new Random().nextInt(200) == 0 &&
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
                sendError(event, command.getLocale(), eb);
            } else if (BotPermissionUtil.canWrite(event.getChannel())) {
                sendErrorNoEmbed(event, command.getLocale(), desc);
            }
        }

        return false;
    }

    private static boolean checkCoolDown(GuildMessageReceivedEvent event, Command command) throws InterruptedException {
        if (PatreonCache.getInstance().getUserTier(event.getMember().getIdLong()) >= 3)
            return true;
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
                sendError(event, command.getLocale(), eb);
            } else if (BotPermissionUtil.canWrite(event.getChannel())) {
                sendErrorNoEmbed(event, command.getLocale(), desc);
            }

            Thread.sleep(5000);
        }

        return false;
    }

    private static boolean checkReleased(GuildMessageReceivedEvent event, Command command) {
        LocalDate releaseDate = command.getReleaseDate().orElse(LocalDate.now());
        if (!releaseDate.isAfter(LocalDate.now()) || PatreonCache.getInstance().getUserTier(event.getMember().getIdLong()) > 1) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_description", ExternalLinks.PATREON_PAGE);
        String waitTime = TextManager.getString(command.getLocale(), TextManager.GENERAL, "next", TimeUtil.getRemainingTimeString(command.getLocale(), Instant.now(), TimeUtil.localDateToInstant(releaseDate), false));

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_beta_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            EmbedUtil.addLog(eb, LogStatus.TIME, waitTime);
            sendError(event, command.getLocale(), eb);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc + "\n\n`" + waitTime + "`");
        }

        return false;
    }

    private static boolean checkPatreon(GuildMessageReceivedEvent event, Command command) {
        if (!command.getCommandProperties().patreonRequired() || PatreonCache.getInstance().getUserTier(event.getMember().getIdLong()) > 1) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_description", ExternalLinks.PATREON_PAGE);

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setColor(Settings.PATREON_COLOR)
                    .setAuthor(TextManager.getString(command.getLocale(), TextManager.GENERAL, "patreon_title"), ExternalLinks.PATREON_PAGE, "https://c5.patreon.com/external/favicon/favicon-32x32.png?v=69kMELnXkB")
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb);
        } else if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc);
        }

        return false;
    }

    private static boolean checkPermissions(GuildMessageReceivedEvent event, Command command) {
        EmbedBuilder errEmbed = BotPermissionUtil.getUserAndBotPermissionMissingEmbed(
                command.getLocale(),
                event.getChannel(),
                event.getMember(),
                command.getUserGuildPermissions(),
                command.getUserChannelPermissions(),
                command.getBotPermissions()
        );
        if (errEmbed == null || command instanceof HelpCommand) {
            return true;
        }

        if (BotPermissionUtil.canWriteEmbed(event.getChannel()))
            sendError(event, command.getLocale(), errEmbed);
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
            sendError(event, command.getLocale(), eb);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc);
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

        sendErrorNoEmbed(event, command.getLocale(), TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_embed"));
        return false;
    }

    private static boolean isNSFWCompliant(GuildMessageReceivedEvent event, Command command) {
        if (!command.getCommandProperties().nsfw() || event.getChannel().isNSFW()) {
            return true;
        }

        EmbedBuilder eb = EmbedFactory.getNSFWBlockEmbed(command.getLocale());
        sendError(event, command.getLocale(), eb);
        return false;
    }

    private static void sendErrorNoEmbed(GuildMessageReceivedEvent event, Locale locale, String text) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            event.getMessage()
                    .reply(TextManager.getString(locale, TextManager.GENERAL, "command_block", text, event.getMember().getAsMention()))
                    .queue(message -> autoRemoveMessageAfterCountdown(event, message));
        }
    }

    private static void sendError(GuildMessageReceivedEvent event, Locale locale, EmbedBuilder eb) {
        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            eb.setFooter(TextManager.getString(locale, TextManager.GENERAL, "deleteTime", String.valueOf(SEC_UNTIL_REMOVAL)));
            event.getMessage().reply(
                    new EmbedWithContent(event.getMessage().getMember().getAsMention(), eb.build()).build()
            ).queue(message -> autoRemoveMessageAfterCountdown(event, message));
        }
    }

    private static void autoRemoveMessageAfterCountdown(GuildMessageReceivedEvent event, Message message) {
        MainScheduler.getInstance().schedule(SEC_UNTIL_REMOVAL, ChronoUnit.SECONDS, "command_manager_error_countdown", () -> {
            if (BotPermissionUtil.can(event.getMember(), Permission.MESSAGE_MANAGE))
                event.getChannel().deleteMessages(Arrays.asList(message, event.getMessage())).queue(); //audit lot?
            else
                message.delete().queue();
        });
    }

    private static boolean isWhiteListed(GuildMessageReceivedEvent event, Command command) {
        if (BotPermissionUtil.can(event.getMember(), Permission.MANAGE_SERVER) ||
                DBWhiteListedChannels.getInstance().retrieve(event.getGuild().getIdLong()).isWhiteListed(event.getChannel().getIdLong())
        ) {
            return true;
        }

        String desc = TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_description");

        if (BotPermissionUtil.canWriteEmbed(event.getChannel())) {
            EmbedBuilder eb = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(command.getLocale(), TextManager.GENERAL, "whitelist_title", command.getPrefix()))
                    .setDescription(desc);
            sendError(event, command.getLocale(), eb);
        } else if (BotPermissionUtil.canWrite(event.getChannel())) {
            sendErrorNoEmbed(event, command.getLocale(), desc);
        }
        return false;
    }

    private static boolean botCanPost(GuildMessageReceivedEvent event, Command command) {
        if (BotPermissionUtil.canWrite(event.getChannel()) || command instanceof HelpCommand) {
            return true;
        }

        if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_ADD_REACTION)) {
            if (BotPermissionUtil.can(event.getChannel(), Permission.MESSAGE_EXT_EMOJI))
                event.getMessage().addReaction(Emojis.NO).queue();
            else
                event.getMessage().addReaction("❌").queue();
            event.getMessage().addReaction("✍️").queue();
        }

        if (BotPermissionUtil.can(event.getMember(), Permission.ADMINISTRATOR)) {
            JDAUtil.sendPrivateMessage(
                    event.getMember(),
                    TextManager.getString(command.getLocale(), TextManager.GENERAL, "no_writing_permissions", StringUtil.escapeMarkdown(event.getChannel().getName()))
            ).queue();
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
        //TODO: check if it works
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
