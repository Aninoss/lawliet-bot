package commands.listeners

import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent

interface OnStaticStringSelectMenuListener {

    @Throws(Throwable::class)
    fun onStaticStringSelectMenu(event: StringSelectInteractionEvent, secondaryId: String?)

}