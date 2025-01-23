package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import mysql.hibernate.EntityManagerWrapper
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import java.util.concurrent.CompletableFuture

interface OnEntitySelectMenuListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onEntitySelectMenu(event: EntitySelectInteractionEvent): Boolean

    fun registerEntitySelectMenuListener(member: Member): CompletableFuture<Long> {
        return registerEntitySelectMenuListener(member, true)
    }

    fun registerEntitySelectMenuListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnEntitySelectMenuListener::class.java, draw) { onEntitySelectMenuOverridden() }
    }

    fun registerEntitySelectMenuListener(member: Member, validityChecker: (EntitySelectInteractionEvent) -> CheckResponse, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnEntitySelectMenuListener::class.java, draw, { onEntitySelectMenuOverridden() }, validityChecker)
    }

    fun processEntitySelectMenu(event: EntitySelectInteractionEvent, entityManager: EntityManagerWrapper) {
        processInteraction(event, entityManager) { onEntitySelectMenu(it) }
    }

    @Throws(Throwable::class)
    fun onEntitySelectMenuOverridden() {
    }

}