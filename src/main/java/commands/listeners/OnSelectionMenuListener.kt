package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent
import java.util.concurrent.CompletableFuture

interface OnSelectionMenuListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onSelectionMenu(event: SelectionMenuEvent): Boolean

    fun registerSelectionMenuListener(member: Member): CompletableFuture<Long> {
        return registerSelectionMenuListener(member, true)
    }

    fun registerSelectionMenuListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnSelectionMenuListener::class.java, draw) { onSelectionMenuOverridden() }
    }

    fun registerSelectionMenuListener(member: Member, validityChecker: (SelectionMenuEvent) -> CheckResponse, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnSelectionMenuListener::class.java, draw, { onSelectionMenuOverridden() }, validityChecker)
    }

    fun processSelectionMenu(event: SelectionMenuEvent) {
        processInteraction(event) { onSelectionMenu(it) }
    }

    @Throws(Throwable::class)
    fun onSelectionMenuOverridden() {
    }

}