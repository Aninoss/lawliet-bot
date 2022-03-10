package commands

import core.utils.JDAUtil
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.channel.GenericChannelEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.requests.RestAction

class CommandEvent : GenericChannelEvent {

    val member: Member
    val slashCommandInteractionEvent: SlashCommandInteractionEvent?
    val messageReceivedEvent: MessageReceivedEvent?
    val user: User
        get() = member.user
    val repliedMember: Member?
        get() = messageReceivedEvent?.message?.messageReference?.message?.member
    val textChannel: TextChannel
        get() {
            val channel = getChannel()
            if (channel is TextChannel) {
                return channel
            }
            throw IllegalStateException("Cannot convert channel of type $channelType to TextChannel")
        }

    constructor(event: SlashCommandInteractionEvent) : super(event.jda, event.responseNumber, event.textChannel) {
        slashCommandInteractionEvent = event
        messageReceivedEvent = null
        member = event.member!!
    }

    constructor(event: MessageReceivedEvent) : super(event.jda, event.responseNumber, event.textChannel) {
        slashCommandInteractionEvent = null
        messageReceivedEvent = event
        member = event.member!!
    }

    fun isSlashCommandInteractionEvent(): Boolean {
        return slashCommandInteractionEvent != null
    }

    fun isMessageReceivedEvent(): Boolean {
        return messageReceivedEvent != null
    }

    fun replyMessage(content: String, actionRows: Collection<ActionRow>): RestAction<Message> {
        return if (isMessageReceivedEvent()) {
            JDAUtil.replyMessage(messageReceivedEvent!!.message, content)
                .setActionRows(actionRows)
        } else {
            slashCommandInteractionEvent!!.hook.sendMessage(content)
                .addActionRows(actionRows)
        }
    }

    fun replyMessageEmbeds(embeds: List<MessageEmbed>, actionRows: Collection<ActionRow>): RestAction<Message> {
        return if (isMessageReceivedEvent()) {
            JDAUtil.replyMessageEmbeds(messageReceivedEvent!!.message, embeds)
                .setActionRows(actionRows)
        } else {
            slashCommandInteractionEvent!!.hook.sendMessageEmbeds(embeds)
                .addActionRows(actionRows)
        }
    }

}