package commands.listeners

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

interface OnStaticReactionAddListener {

    @Throws(Throwable::class)
    fun onStaticReactionAdd(message: Message, event: MessageReactionAddEvent)

}