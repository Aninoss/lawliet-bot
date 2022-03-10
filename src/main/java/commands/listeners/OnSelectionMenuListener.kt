package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent
import java.util.concurrent.CompletableFuture

interface OnSelectMenuListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onSelectMenu(event: SelectMenuInteractionEvent): Boolean

    fun registerSelectMenuListener(member: Member): CompletableFuture<Long> {
        return registerSelectMenuListener(member, true)
    }

    fun registerSelectMenuListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnSelectMenuListener::class.java, draw) { onSelectMenuOverridden() }
    }

    fun registerSelectMenuListener(member: Member, validityChecker: (SelectMenuInteractionEvent) -> CheckResponse, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnSelectMenuListener::class.java, draw, { onSelectMenuOverridden() }, validityChecker)
    }

    fun processSelectMenu(event: SelectMenuInteractionEvent) {
        processInteraction(event) { onSelectMenu(it) }
    }

    @Throws(Throwable::class)
    fun onSelectMenuOverridden() {
    }

}