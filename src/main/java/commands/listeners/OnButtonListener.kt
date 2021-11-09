package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent
import java.util.concurrent.CompletableFuture
import java.util.function.Function

interface OnButtonListener : OnInteractionListener {

    @Throws(Throwable::class)
    fun onButton(event: ButtonClickEvent): Boolean

    fun registerButtonListener(member: Member): CompletableFuture<Long> {
        return registerButtonListener(member, true)
    }

    fun registerButtonListener(member: Member, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnButtonListener::class.java, draw, { onButtonOverridden() })
    }

    fun registerButtonListener(member: Member, draw: Boolean, validityChecker: (ButtonClickEvent) -> CheckResponse): CompletableFuture<Long> {
        return registerInteractionListener(member, OnButtonListener::class.java, draw, { onButtonOverridden() }, validityChecker)
    }

    fun processButton(event: ButtonClickEvent) {
        processInteraction(event) { onButton(it) }
    }

    @Throws(Throwable::class)
    fun onButtonOverridden() {
    }

}