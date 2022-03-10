package commands.slashadapters.adapters

import commands.runnables.fisherysettingscategory.FisheryManageCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = FisheryManageCommand::class)
class FisheryManageAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val components =
            arrayOf("fish", "coins", "dailystreak", "fishing_rod", "fishing_robot", "fishing_net", "metal_detector", "role", "survey", "work", "reset")
        val optionData = OptionData(OptionType.STRING, "component", "Which component should be modified?", false)
        for (component in components) {
            optionData.addChoice(component, component)
        }
        return commandData
            .addOption(OptionType.USER, "member", "Select a member", false)
            .addOption(OptionType.USER, "member2", "Select a member", false)
            .addOption(OptionType.USER, "member3", "Select a member", false)
            .addOption(OptionType.USER, "member4", "Select a member", false)
            .addOption(OptionType.USER, "member5", "Select a member", false)
            .addOption(OptionType.STRING, "member_id", "Select a member", false)
            .addOption(OptionType.ROLE, "role", "Select all members of a role", false)
            .addOptions(optionData)
            .addOption(OptionType.STRING, "operation", "What operation should be performed on the component? (e.g. +1, -4, 6)")
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(FisheryManageCommand::class.java, collectArgs(event))
    }

}