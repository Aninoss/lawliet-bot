package commands

import core.utils.JDAUtil
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.channel.text.GenericTextChannelEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.requests.RestAction

class CommandEvent : GenericTextChannelEvent {

    val member: Member
    val slashCommandEvent: SlashCommandEvent?
    val guildMessageReceivedEvent: GuildMessageReceivedEvent?
    val user: User
        get() = member.user
    val repliedMember: Member?
        get() = guildMessageReceivedEvent?.message?.messageReference?.message?.member

    constructor(event: SlashCommandEvent) : super(event.jda, event.responseNumber, event.textChannel) {
        slashCommandEvent = event
        guildMessageReceivedEvent = null
        member = event.member!!
    }

    constructor(event: GuildMessageReceivedEvent) : super(event.jda, event.responseNumber, event.channel) {
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