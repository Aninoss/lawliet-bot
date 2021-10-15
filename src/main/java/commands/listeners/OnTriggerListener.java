package commands.listeners;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import commands.Command;
import commands.CommandContainer;
import commands.CommandEvent;
import commands.runnables.utilitycategory.TriggerDeleteCommand;
import core.MemberCacheController;
import core.PermissionCheckRuntime;
import core.Program;
import core.interactionresponse.InteractionResponse;
import core.interactionresponse.SlashCommandResponse;
import core.schedule.MainScheduler;
import core.utils.ExceptionUtil;
import mysql.modules.commandusages.DBCommandUsages;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public interface OnTriggerListener {

    boolean onTrigger(CommandEvent event, String args) throws Throwable;

    default boolean processTrigger(CommandEvent event, String args, boolean freshCommand) throws Throwable {
        Command command = (Command) this;
        if (freshCommand && event.isSlashCommandEvent()) {
            InteractionResponse interactionResponse = new SlashCommandResponse(event.getSlashCommandEvent().getHook());
            command.setInteractionResponse(interactionResponse);
        }

        AtomicBoolean isProcessing = new AtomicBoolean(true);
        command.setAtomicAssets(event.getChannel(), event.getMember());
        command.setCommandEvent(event);

        if (Program.publicVersion()) {
            DBCommandUsages.getInstance().retrieve(command.getTrigger()).increase();
        }

        if (event.isGuildMessageReceivedEvent()) {
            command.addLoadingReaction(event.getGuildMessageReceivedEvent().getMessage(), isProcessing);
            processTriggerDelete(event.getGuildMessageReceivedEvent());
        }
        addKillTimer(isProcessing);
        try {
            if (command.getCommandProperties().requiresFullMemberCache()) {
                MemberCacheController.getInstance().loadMembersFull(event.getGuild()).get();
            }
            return onTrigger(event, args);
        } catch (Throwable e) {
            ExceptionUtil.handleCommandException(e, command);
            return false;
        } finally {
            isProcessing.set(false);
        }
    }

    private void addKillTimer(AtomicBoolean isProcessing) {
        Command command = (Command) this;
        Thread commandThread = Thread.currentThread();
        MainScheduler.schedule(command.getCommandProperties().maxCalculationTimeSec(), ChronoUnit.SECONDS, "command_timeout", () -> {
            if (!command.getCommandProperties().turnOffTimeout()) {
                CommandContainer.addCommandTerminationStatus(command, commandThread, isProcessing.get());
            }
        });
    }

    private void processTriggerDelete(GuildMessageReceivedEvent event) {
        GuildData guildBean = DBGuild.getInstance().retrieve(event.getGuild().getIdLong());
        if (guildBean.isCommandAuthorMessageRemoveEffectively() &&
                PermissionCheckRuntime.botHasPermission(guildBean.getLocale(), TriggerDeleteCommand.class, event.getChannel(), Permission.MESSAGE_MANAGE)
        ) {
            event.getMessage().delete().queue();
        }
    }

}