package commands.slashadapters.adapters

import commands.runnables.fisherycategory.BuyCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = BuyCommand::class)
class BuyAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val components = arrayOf("fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work")
        val optionData = generateOptionData(OptionType.STRING, "gear", "buy_gear", false)
        components.forEachIndexed { i, component ->
            val choice = generateChoice("buy_product_${i}_0", component)
            optionData.addChoices(choice)
        }
        return commandData
            .addOptions(
                optionData,
                generateOptionData(OptionType.INTEGER, "amount", "buy_amount", false)
            )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(BuyCommand::class.java, collectArgs(event))
    }

}