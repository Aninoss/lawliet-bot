package commands.listeners

import commands.Command
import commands.CommandContainer
import commands.CommandListenerMeta
import commands.CommandListenerMeta.CheckResponse
import core.ExceptionLogger
import core.MainLogger
import core.MemberCacheController
import core.utils.BotPermissionUtil
import core.utils.ExceptionUtil
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.atomic.AtomicBoolean

interface OnMessageInputListener : Drawable {

    @Throws(Throwable::class)
    fun onMessageInput(event: MessageReceivedEvent, input: String): MessageInputResponse?

    fun registerMessageInputListener(member: Member, draw: Boolean = true) {
        val command = this as Command
        registerMessageInputListener(member, draw) { event ->
            val ok = event.member!!.idLong == member.idLong &&
                    event.channel.idLong == command.textChannelId.orElse(0L)
            if (ok) CheckResponse.ACCEPT else CheckResponse.IGNORE
        }
    }

    fun registerMessageInputListener(member: Member, draw: Boolean, validityChecker: (MessageReceivedEvent) -> CheckResponse) {
        val command = this as Command
        val onTimeOut = {
            try {
                command.refreshGuildEntity().use {
                    command.deregisterListeners()
                    command.onListenerTimeOutSuper()
                }
            } catch (throwable: Throwable) {
                MainLogger.get().error("Exception on time out", throwable)
            }
        }
        val onOverridden = {
            try {
                command.refreshGuildEntity().use {
                    onMessageInputOverridden()
                }
            } catch (throwable: Throwable) {
                MainLogger.get().error("Exception on overridden", throwable)
            }
        }
        val commandListenerMeta = CommandListenerMeta(member.idLong, validityChecker, onTimeOut, onOverridden, command)
        CommandContainer.registerListener(OnMessageInputListener::class.java, commandListenerMeta)
        try {
            if (draw && command.drawMessageId.isEmpty) {
                val eb = draw(member)
                if (eb != null) {
                    command.drawMessage(eb)
                        .exceptionally(ExceptionLogger.get())
                }
            }
        } catch (e: Throwable) {
            command.textChannel.ifPresent { ExceptionUtil.handleCommandException(e, command, commandEvent, guildEntity) }
        }
    }

    fun processMessageInput(event: MessageReceivedEvent, guildEntity: GuildEntity): MessageInputResponse? {
        val command = this as Command
        val isProcessing = AtomicBoolean(true)
        try {
            if (command.commandProperties.requiresFullMemberCache) {
                MemberCacheController.getInstance().loadMembersFull(event.guild).get()
            }
            command.guildEntity = guildEntity
            val messageInputResponse = onMessageInput(event, event.message.contentRaw)
            if (messageInputResponse != null) {
                if (messageInputResponse === MessageInputResponse.SUCCESS) {
                    CommandContainer.refreshListeners(command)
                    if (BotPermissionUtil.can(event.guildChannel, Permission.MESSAGE_MANAGE)) {
                        event.message.delete().queue()
                    }
                }
                val eb = draw(event.member!!)
                if (eb != null) {
                    (this as Command).drawMessage(eb).exceptionally(ExceptionLogger.get())
                }
            }
            return messageInputResponse
        } catch (e: Throwable) {
            ExceptionUtil.handleCommandException(e, command, commandEvent, guildEntity)
            return MessageInputResponse.ERROR
        } finally {
            isProcessing.set(false)
        }
    }

    @Throws(Throwable::class)
    fun onMessageInputOverridden() {
    }

}