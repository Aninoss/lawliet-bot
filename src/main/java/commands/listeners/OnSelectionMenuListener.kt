package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import mysql.hibernate.EntityManagerWrapper
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import java.util.concurrent.CompletableFuture

interface OnSelectMenuListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onSelectMenu(event: StringSelectInteractionEvent): Boolean

    fun registerSelectMenuListener(member: Member): CompletableFuture<Long> {
        return registerSelectMenuListener(member, true)
    }

    fun registerSelectMenuListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnSelectMenuListener::class.java, draw) { onSelectMenuOverridden() }
    }

    fun registerSelectMenuListener(member: Member, validityChecker: (StringSelectInteractionEvent) -> CheckResponse, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnSelectMenuListener::class.java, draw, { onSelectMenuOverridden() }, validityChecker)
    }

    fun processSelectMenu(event: StringSelectInteractionEvent, entityManager: EntityManagerWrapper) {
        processInteraction(event, entityManager) { onSelectMenu(it) }
    }

    @Throws(Throwable::class)
    fun onSelectMenuOverridden() {
    }

}