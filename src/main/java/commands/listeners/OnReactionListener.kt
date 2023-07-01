package commands.listeners

import commands.Command
import commands.CommandContainer
import commands.CommandListenerMeta
import commands.CommandListenerMeta.CheckResponse
import core.ExceptionLogger
import core.MainLogger
import core.MemberCacheController
import core.RestActionQueue
import core.utils.BotPermissionUtil
import core.utils.EmojiUtil
import core.utils.ExceptionUtil
import mysql.hibernate.EntityManagerWrapper
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.exceptions.PermissionException
import java.util.*
import java.util.concurrent.CompletableFuture

interface OnReactionListener : Drawable {

    @Throws(Throwable::class)
    fun onReaction(event: GenericMessageReactionEvent): Boolean

    fun registerReactionListener(member: Member, vararg emojis: Emoji): CompletableFuture<Long> {
        val command = this as Command
        return registerReactionListener(member) { event: GenericMessageReactionEvent ->
            val ok = event.userIdLong == member.idLong && event.messageIdLong == (this as Command).drawMessageId.orElse(0L) &&
                    (emojis.size == 0 || Arrays.stream(emojis).anyMatch { emoji -> EmojiUtil.equals(event.emoji, emoji) })
            if (ok) CheckResponse.ACCEPT else CheckResponse.IGNORE
        }.thenApply { messageId: Long ->
            command.textChannel.ifPresent { channel: TextChannel ->
                val restActionQueue = RestActionQueue()
                Arrays.stream(emojis).forEach { emoji -> restActionQueue.attach(channel.addReactionById(messageId, emoji)) }
                if (restActionQueue.isSet) {
                    restActionQueue.currentRestAction.queue()
                }
            }
            messageId
        }
    }

    fun registerReactionListener(member: Member, validityChecker: (GenericMessageReactionEvent) -> CheckResponse): CompletableFuture<Long> {
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
                onReactionOverridden()
            } catch (throwable: Throwable) {
                MainLogger.get().error("Exception on overridden", throwable)
            }
        }
        val commandListenerMeta = CommandListenerMeta(member.idLong, validityChecker, onTimeOut, onOverridden, command)
        CommandContainer.registerListener(OnReactionListener::class.java, commandListenerMeta)
        try {
            if (command.drawMessageId.isEmpty) {
                val eb = draw(member)
                if (eb != null) {
                    return command.drawMessage(eb)
                        .thenApply { obj: Message -> obj.idLong }
                        .exceptionally(ExceptionLogger.get())
                }
            } else {
                return CompletableFuture.completedFuture(command.drawMessageId.get())
            }
        } catch (e: Throwable) {
            command.textChannel.ifPresent { ExceptionUtil.handleCommandException(e, command, commandEvent) }
        }
        return CompletableFuture.failedFuture(NoSuchElementException("No message sent"))
    }

    fun deregisterListenersWithReactionMessage() {
        val command = this as Command
        command.drawMessageId.ifPresent { messageId: Long ->
            command.textChannel.ifPresent { channel: TextChannel ->
                if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_MANAGE) && command.commandEvent.isMessageReceivedEvent()) {
                    val messageIds: Collection<String> = listOf(messageId.toString(), command.commandEvent!!.messageReceivedEvent!!.messageId)
                    channel.deleteMessagesByIds(messageIds).queue()
                } else if (BotPermissionUtil.canReadHistory(channel)) {
                    channel.deleteMessageById(messageId).queue()
                }
            }
        }
        command.deregisterListeners()
    }

    fun deregisterListenersWithReactions(): CompletableFuture<Void?> {
        val future = CompletableFuture<Void?>()
        val command = this as Command
        command.drawMessageId.ifPresentOrElse({ messageId: Long ->
            command.textChannel.ifPresentOrElse({ channel: TextChannel ->
                if (BotPermissionUtil.canReadHistory(channel, Permission.MESSAGE_MANAGE)) {
                    channel.clearReactionsById(messageId)
                        .queue({ future.complete(null) }) { ex: Throwable -> future.completeExceptionally(ex) }
                } else {
                    future.completeExceptionally(PermissionException("Missing permissions"))
                }
            }) { future.completeExceptionally(NoSuchElementException("No such text channel")) }
        }) { future.completeExceptionally(NoSuchElementException("No such draw message id")) }
        command.deregisterListeners()
        return future
    }

    fun processReaction(event: GenericMessageReactionEvent, entityManager: EntityManagerWrapper) {
        val command = this as Command
        try {
            if (command.commandProperties.requiresFullMemberCache) {
                MemberCacheController.getInstance().loadMembersFull(event.guild).get()
            } else if (event is MessageReactionRemoveEvent) {
                MemberCacheController.getInstance().loadMember(event.getGuild(), event.getUserIdLong()).get()
            }
            if (event.user == null || event.user!!.isBot) {
                return
            }
            command.guildEntity = guildEntity
            if (onReaction(event)) {
                CommandContainer.refreshListeners(command)
                val eb = draw(event.member!!)
                if (eb != null) {
                    (this as Command).drawMessage(eb)
                        .exceptionally(ExceptionLogger.get())
                }
            }
        } catch (e: Throwable) {
            ExceptionUtil.handleCommandException(e, command, commandEvent)
        }
    }

    @Throws(Throwable::class)
    fun onReactionOverridden() {
    }

}