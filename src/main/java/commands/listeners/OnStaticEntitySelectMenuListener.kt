package commands.listeners

import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent

interface OnStaticEntitySelectMenuListener {

    @Throws(Throwable::class)
    fun onStaticEntitySelectMenu(event: EntitySelectInteractionEvent, secondaryId: String?)

}