package commands.slashadapters.adapters

import commands.runnables.fisherysettingscategory.PowerUpCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = PowerUpCommand::class)
class PowerUpAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.USER, "member", "powerup_member", true),
            generateOptionData(OptionType.CHANNEL, "channel", "powerup_channel", false),
            generateOptionData(OptionType.INTEGER, "amount", "powerup_amount", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(PowerUpCommand::class.java, collectArgs(event))
    }

}