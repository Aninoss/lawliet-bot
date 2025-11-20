package commands

import com.google.common.collect.Lists
import core.schedule.MainScheduler
import core.slashmessageaction.SlashAckSendMessageAction
import core.slashmessageaction.SlashHookSendMessageAction
import core.utils.JDAUtil
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.components.tree.MessageComponentTree
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel
import net.dv8tion.jda.api.events.channel.GenericChannelEvent
import net.dv8tion.jda.api.events.interaction.command.GenericCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

class CommandEvent : GenericChannelEvent {

    val member: Member
    val genericCommandInteractionEvent: GenericCommandInteractionEvent?
    val messageReceivedEvent: MessageReceivedEvent?
    val user: User
        get() = member.user
    val repliedMember: Member?
        get() = messageReceivedEvent?.message?.messageReference?.message?.member
    val messageChannel: GuildMessageChannel
        get() {
            val channel = getChannel()
            if (channel is GuildMessageChannel) {
                return channel
            }
            throw IllegalStateException("Cannot convert channel of type $channelType to GuildMessageChannel")
        }
    var ack = false
    var attachments: List<Message.Attachment>

    constructor(event: GenericCommandInteractionEvent) : super(event.jda, event.responseNumber, event.channel) {
        genericCommandInteractionEvent = event
        messageReceivedEvent = null
        member = event.member!!
        attachments = event.options
            .filter { it.type == OptionType.ATTACHMENT }
            .map { it.asAttachment }
    }

    constructor(event: MessageReceivedEvent) : super(event.jda, event.responseNumber, event.channel) {
        genericCommandInteractionEvent = null
        messageReceivedEvent = event
        member = event.member!!
        attachments = event.message.attachments
    }

    fun isGenericCommandInteractionEvent(): Boolean {
        return genericCommandInteractionEvent != null
    }

    fun isMessageReceivedEvent(): Boolean {
        return messageReceivedEvent != null
    }

    fun replyMessage(guildEntity: GuildEntity, ephemeral: Boolean = false, content: String): MessageCreateAction {
        if (isMessageReceivedEvent()) {
            return JDAUtil.replyMessage(messageReceivedEvent!!.message, guildEntity, content)
        } else {
            if (genericCommandInteractionEvent!!.isAcknowledged) {
                return SlashHookSendMessageAction(genericCommandInteractionEvent.hook.sendMessage(content).setEphemeral(ephemeral))
            } else {
                return SlashAckSendMessageAction(genericCommandInteractionEvent.reply(content).setEphemeral(ephemeral))
            }
        }
    }

    fun replyMessageEmbeds(guildEntity: GuildEntity, ephemeral: Boolean = false, embed: MessageEmbed, vararg other: MessageEmbed): MessageCreateAction {
        return replyMessageEmbeds(guildEntity, ephemeral, Lists.asList(embed, other))
    }

    fun replyMessageEmbeds(guildEntity: GuildEntity, ephemeral: Boolean = false, embeds: Collection<MessageEmbed>): MessageCreateAction {
        if (isMessageReceivedEvent()) {
            return JDAUtil.replyMessageEmbeds(messageReceivedEvent!!.message, guildEntity, embeds)
        } else {
            if (genericCommandInteractionEvent!!.isAcknowledged) {
                return SlashHookSendMessageAction(genericCommandInteractionEvent.hook.sendMessageEmbeds(embeds).setEphemeral(ephemeral))
            } else {
                return SlashAckSendMessageAction(genericCommandInteractionEvent.replyEmbeds(embeds).setEphemeral(ephemeral))
            }
        }
    }

    fun replyMessageComponents(guildEntity: GuildEntity, ephemeral: Boolean = false, componentTree: MessageComponentTree): MessageCreateAction {
        if (isMessageReceivedEvent()) {
            return JDAUtil.replyMessageComponents(messageReceivedEvent!!.message, guildEntity, componentTree)
        } else {
            if (genericCommandInteractionEvent!!.isAcknowledged) {
                return SlashHookSendMessageAction(genericCommandInteractionEvent.hook.sendMessageComponents(componentTree).useComponentsV2(true).setEphemeral(ephemeral))
            } else {
                return SlashAckSendMessageAction(genericCommandInteractionEvent.replyComponents(componentTree).useComponentsV2(true).setEphemeral(ephemeral))
            }
        }
    }

    @JvmOverloads
    fun deferReply(ephemeral: Boolean = false, processing: AtomicBoolean) {
        if (ack) {
            return
        }
        ack = true

        genericCommandInteractionEvent?.let {
            if (!it.isAcknowledged) {
                it.deferReply().setEphemeral(ephemeral).queue()
            }
        }
        sendTyping(processing)
    }

    private fun sendTyping(processing: AtomicBoolean) {
        if (!processing.get()) {
            return
        }

        messageReceivedEvent?.guildChannel?.sendTyping()?.queue {
            MainScheduler.schedule(Duration.ofSeconds(9)) { sendTyping(processing) }
        }
    }

}