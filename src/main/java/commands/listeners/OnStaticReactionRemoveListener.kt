package commands.listeners

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent

interface OnStaticReactionRemoveListener {

    @Throws(Throwable::class)
    fun onStaticReactionRemove(message: Message, event: MessageReactionRemoveEvent)

}