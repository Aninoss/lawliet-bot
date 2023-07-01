package commands.slashadapters.adapters

import commands.runnables.fisherysettingscategory.AutoClaimCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = AutoClaimCommand::class)
class AutoClaimAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData
            .addOptions(
                generateOptionData(OptionType.BOOLEAN, "active", "fisheryset_active", false)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        var args = ""
        if (event.getOption("active") != null) {
            args = if (event.getOption("active")!!.asBoolean) "on" else "off"
        }
        return SlashMeta(AutoClaimCommand::class.java, args)
    }

}