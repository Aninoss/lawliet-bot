package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import mysql.hibernate.EntityManagerWrapper
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent
import java.util.concurrent.CompletableFuture

interface OnButtonListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onButton(event: ButtonInteractionEvent): Boolean

    fun registerButtonListener(member: Member): CompletableFuture<Long> {
        return registerButtonListener(member, true)
    }

    fun registerButtonListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnButtonListener::class.java, draw, { onButtonOverridden() })
    }

    fun registerButtonListener(member: Member, draw: Boolean, validityChecker: (GenericComponentInteractionCreateEvent) -> CheckResponse): CompletableFuture<Long> {
        return registerInteractionListener(member, OnButtonListener::class.java, draw, { onButtonOverridden() }, validityChecker)
    }

    fun processButton(event: ButtonInteractionEvent, entityManager: EntityManagerWrapper) {
        processInteraction(event, entityManager) { onButton(it) }
    }

    @Throws(Throwable::class)
    fun onButtonOverridden() {
    }

}