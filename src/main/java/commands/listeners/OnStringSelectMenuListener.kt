package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import mysql.hibernate.EntityManagerWrapper
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import java.util.concurrent.CompletableFuture

interface OnStringSelectMenuListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onStringSelectMenu(event: StringSelectInteractionEvent): Boolean

    fun registerStringSelectMenuListener(member: Member): CompletableFuture<Long> {
        return registerStringSelectMenuListener(member, true)
    }

    fun registerStringSelectMenuListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnStringSelectMenuListener::class.java, draw) { onStringSelectMenuOverridden() }
    }

    fun registerStringSelectMenuListener(member: Member, draw: Boolean, validityChecker: (GenericComponentInteractionCreateEvent) -> CheckResponse): CompletableFuture<Long> {
        return registerInteractionListener(member, OnStringSelectMenuListener::class.java, draw, { onStringSelectMenuOverridden() }, validityChecker)
    }

    fun processStringSelectMenu(event: StringSelectInteractionEvent, entityManager: EntityManagerWrapper) {
        processInteraction(event, entityManager) { onStringSelectMenu(it) }
    }

    @Throws(Throwable::class)
    fun onStringSelectMenuOverridden() {
    }

}