package commands.slashadapters.adapters

import commands.Category
import commands.runnables.invitetrackingcategory.InvitesTopCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = InvitesTopCommand::class)
class InvitesTopAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        val properties = arrayOf("total", "on_server", "retained", "active")
        val optionData = OptionData(OptionType.STRING, "sort_by", "Which property should determine the ranking?", false)
        properties.forEachIndexed() { i, property ->
            val name = TextManager.getString(Language.EN.locale, Category.INVITE_TRACKING, "invtop_orderby").split("\n")[i]
            optionData.addChoice(name, property)
        }

        return commandData
            .addOptions(optionData)
            .addOption(OptionType.INTEGER, "page", "Which page to view", false)
    }

    override fun process(event: SlashCommandInteractionEvent): SlashMeta {
        return SlashMeta(InvitesTopCommand::class.java, collectArgs(event))
    }

}