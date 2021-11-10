package commands.listeners

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent

interface OnStaticReactionRemoveListener {

    @Throws(Throwable::class)
    fun onStaticReactionRemove(message: Message, event: GuildMessageReactionRemoveEvent)

}