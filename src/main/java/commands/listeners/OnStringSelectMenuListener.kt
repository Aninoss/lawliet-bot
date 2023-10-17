package commands.listeners

import commands.CommandListenerMeta.CheckResponse
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.entities.Member
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

    fun registerStringSelectMenuListener(member: Member, validityChecker: (StringSelectInteractionEvent) -> CheckResponse, draw: Boolean): CompletableFuture<Long> {
        return registerInteractionListener(member, OnStringSelectMenuListener::class.java, draw, { onStringSelectMenuOverridden() }, validityChecker)
    }

    fun processStringSelectMenu(event: StringSelectInteractionEvent, guildEntity: GuildEntity) {
        processInteraction(event, guildEntity) { onStringSelectMenu(it) }
    }

    @Throws(Throwable::class)
    fun onStringSelectMenuOverridden() {
    }

}