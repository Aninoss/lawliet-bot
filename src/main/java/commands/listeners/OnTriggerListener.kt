package commands.listeners

import commands.Command
import commands.CommandContainer
import commands.CommandEvent
import commands.runnables.utilitycategory.TriggerDeleteCommand
import core.MemberCacheController
import core.PermissionCheckRuntime
import core.Program
import core.featurelogger.FeatureLogger
import core.featurelogger.PremiumFeature
import core.interactionresponse.InteractionResponse
import core.interactionresponse.SlashCommandResponse
import core.schedule.MainScheduler
import core.utils.ExceptionUtil
import mysql.hibernate.entity.GuildEntity
import mysql.modules.commandusages.DBCommandUsages
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

interface OnTriggerListener {

    @Throws(Throwable::class)
    fun onTrigger(event: CommandEvent, args: String): Boolean

    @Throws(Throwable::class)
    fun processTrigger(event: CommandEvent, args: String, guildEntity: GuildEntity, freshCommand: Boolean): Boolean {
        val command = this as Command
        if (freshCommand && event.isSlashCommandInteractionEvent()) {
            val interactionResponse: InteractionResponse = SlashCommandResponse(event.slashCommandInteractionEvent!!.hook)
            command.interactionResponse = interactionResponse
        }
        val isProcessing = AtomicBoolean(true)
        command.setAtomicAssets(event.textChannel, event.member)
        command.commandEvent = event
        if (Program.publicVersion()) {
            DBCommandUsages.getInstance().retrieve(command.trigger).increase()
        }
        if (event.isMessageReceivedEvent()) {
            processTriggerDelete(event.messageReceivedEvent!!, guildEntity)
        }
        addKillTimer(isProcessing)
        try {
            if (command.commandProperties.requiresFullMemberCache) {
                MemberCacheController.getInstance().loadMembersFull(event.guild).get()
            }
            command.guildEntity = guildEntity
            return onTrigger(event, args)
        } catch (e: Throwable) {
            ExceptionUtil.handleCommandException(e, command, event, guildEntity)
            return false
        } finally {
            isProcessing.set(false)
        }
    }

    private fun addKillTimer(isProcessing: AtomicBoolean) {
        val command = this as Command
        val commandThread = Thread.currentThread()
        MainScheduler.schedule(Duration.ofSeconds(command.commandProperties.maxCalculationTimeSec.toLong())) {
            if (command.commandProperties.enableCacheWipe) {
                CommandContainer.addCommandTerminationStatus(command, commandThread, isProcessing.get())
            }
        }
    }

    private fun processTriggerDelete(event: MessageReceivedEvent, guildEntity: GuildEntity) {
        if (guildEntity.removeAuthorMessageEffectively &&
            PermissionCheckRuntime.botHasPermission(guildEntity.locale, TriggerDeleteCommand::class.java, event.guildChannel, Permission.MESSAGE_MANAGE)
        ) {
            FeatureLogger.inc(PremiumFeature.TRIGGER_DELETE, event.guild.idLong)
            event.message.delete().queue()
        }
    }

}