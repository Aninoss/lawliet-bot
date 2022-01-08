package commands.listeners

import commands.Command
import commands.CommandContainer
import commands.CommandListenerMeta
import commands.CommandListenerMeta.CheckResponse
import constants.ExceptionFunction
import constants.ExceptionRunnable
import core.ExceptionLogger
import core.MainLogger
import core.MemberCacheController
import core.interactionresponse.ComponentInteractionResponse
import core.interactionresponse.InteractionResponse
import core.utils.BotPermissionUtil
import core.utils.ExceptionUtil
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.interaction.GenericComponentInteractionCreateEvent
import java.util.concurrent.CompletableFuture

interface OnInteractionListener : Drawable {

    @JvmDefault
    fun deregisterListenersWithComponents() {
        val command = this as Command
        command.setActionRows()
        command.deregisterListeners()
    }

    @JvmDefault
    fun deregisterListenersWithComponentMessage() {
        val command = this as Command
        command.drawMessageId.ifPresent { messageId: Long ->
            command.textChannel.ifPresent { channel: TextChannel ->
                if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_MANAGE) && command.commandEvent.isGuildMessageReceivedEvent()) {
                    val messageIds = listOf(messageId.toString(), command.commandEvent.guildMessageReceivedEvent!!.messageId)
                    channel.deleteMessagesByIds(messageIds).queue()
                } else if (BotPermissionUtil.canReadHistory(channel)) {
                    channel.deleteMessageById(messageId).queue()
                }
            }
        }
        command.deregisterListeners()
        command.resetDrawMessage()
    }

    fun registerInteractionListener(member: Member, clazz: Class<*>, draw: Boolean, overriddenMethod: ExceptionRunnable): CompletableFuture<Long> {
        return registerInteractionListener(member, clazz, draw, overriddenMethod) { event: GenericComponentInteractionCreateEvent ->
            if (event.messageIdLong == (this as Command).drawMessageId.orElse(0L)) {
                if (event.user.idLong == member.idLong) {
                    CheckResponse.ACCEPT
                } else {
                    CheckResponse.DENY
                }
            } else {
                CheckResponse.IGNORE
            }
        }
    }

    fun <T : GenericComponentInteractionCreateEvent> registerInteractionListener(member: Member, clazz: Class<*>, draw: Boolean,
                                                                                 overriddenMethod: ExceptionRunnable,
                                                                                 validityChecker: (T) -> CheckResponse
    ): CompletableFuture<Long> {
        val command = this as Command
        val onTimeOut = {
            try {
                command.deregisterListeners()
                command.onListenerTimeOutSuper()
            } catch (throwable: Throwable) {
                MainLogger.get().error("Exception on time out", throwable)
            }
        }
        val onOverridden = {
            try {
                overriddenMethod.run()
            } catch (throwable: Throwable) {
                MainLogger.get().error("Exception on overridden", throwable)
            }
        }
        val commandListenerMeta = CommandListenerMeta(member.idLong, validityChecker, onTimeOut, onOverridden, command)
        CommandContainer.registerListener(clazz, commandListenerMeta)
        try {
            if (draw) {
                if (command.drawMessageId.isEmpty) {
                    val eb = draw(member)
                    if (eb != null) {
                        return command.drawMessage(eb)
                            .thenApply { it.idLong }
                            .exceptionally(ExceptionLogger.get())
                    }
                } else {
                    return CompletableFuture.completedFuture(command.drawMessageId.get())
                }
            }
        } catch (e: Throwable) {
            command.textChannel.ifPresent { ExceptionUtil.handleCommandException(e, command) }
        }
        return CompletableFuture.failedFuture(NoSuchElementException("No message sent"))
    }

    fun <T : GenericComponentInteractionCreateEvent> processInteraction(event: T, task: ExceptionFunction<T, Boolean>) {
        val command = this as Command
        val interactionResponse: InteractionResponse = ComponentInteractionResponse(event)
        command.interactionResponse = interactionResponse
        try {
            if (command.commandProperties.requiresFullMemberCache) {
                MemberCacheController.getInstance().loadMembersFull(event.guild).get()
            }
            if (task.apply(event)) {
                CommandContainer.refreshListeners(command)
                val eb = draw(event.member!!)
                if (eb != null) {
                    (this as Command).drawMessage(eb)
                        .exceptionally(ExceptionLogger.get())
                }
            }
        } catch (e: Throwable) {
            ExceptionUtil.handleCommandException(e, command)
        }
        if (command.drawMessage.isPresent) {
            interactionResponse.complete()
        }
    }

}