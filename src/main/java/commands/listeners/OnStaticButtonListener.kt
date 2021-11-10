package commands.listeners

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent

interface OnStaticButtonListener {

    @Throws(Throwable::class)
    fun onStaticButton(event: ButtonClickEvent)

}