package commands.listeners

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.Member

interface Drawable {

    @Throws(Throwable::class)
    fun draw(member: Member?): EmbedBuilder?

}