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
    val slashCommandEvent: SlashCommandInteractionEvent?
    val guildMessageReceivedEvent: MessageReceivedEvent?
    val user: User
        get() = member.user
    val repliedMember: Member?
        get() = guildMessageReceivedEvent?.message?.messageReference?.message?.member
    val textChannel: TextChannel
        get() = getChannel() as TextChannel

    constructor(event: SlashCommandInteractionEvent) : super(event.jda, event.responseNumber, event.textChannel) {
        slashCommandEvent = event
        guildMessageReceivedEvent = null
        member = event.member!!
    }

    constructor(event: MessageReceivedEvent) : super(event.jda, event.responseNumber, event.textChannel) {
        slashCommandEvent = null
        guildMessageReceivedEvent = event
        member = event.member!!
    }

    fun isSlashCommandEvent(): Boolean {
        return slashCommandEvent != null
    }

    fun isGuildMessageReceivedEvent(): Boolean {
        return guildMessageReceivedEvent != null
    }

    fun replyMessage(content: String, actionRows: Collection<ActionRow>): RestAction<Message> {
        return if (isGuildMessageReceivedEvent()) {
            JDAUtil.replyMessage(guildMessageReceivedEvent!!.message, content)
                .setActionRows(actionRows)
        } else {
            slashCommandEvent!!.hook.sendMessage(content)
                .addActionRows(actionRows)
        }
    }

    fun replyMessageEmbeds(embeds: List<MessageEmbed>, actionRows: Collection<ActionRow>): RestAction<Message> {
        return if (isGuildMessageReceivedEvent()) {
            JDAUtil.replyMessageEmbeds(guildMessageReceivedEvent!!.message, embeds)
                .setActionRows(actionRows)
        } else {
            slashCommandEvent!!.hook.sendMessageEmbeds(embeds)
                .addActionRows(actionRows)
        }
    }

}