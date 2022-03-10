package commands.listeners

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent

interface OnStaticButtonListener {

    @Throws(Throwable::class)
    fun onStaticButton(event: ButtonInteractionEvent)

}