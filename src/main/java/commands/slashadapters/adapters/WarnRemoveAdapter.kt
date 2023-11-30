package commands.slashadapters.adapters

import commands.runnables.moderationcategory.WarnRemoveCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = WarnRemoveCommand::class)
class WarnRemoveAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "members", "warnremove_member", true),
            generateOptionData(OptionType.STRING, "reason", "moderation_reason", false),
            generateOptionData(OptionType.INTEGER, "amount", "warnremove_amount", false),
            generateOptionData(OptionType.BOOLEAN, "all", "warnremove_removeall", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        val args: String
        args = if (event.getOption("all")?.asBoolean ?: false) {
            collectArgs(event, "amount")
        } else {
            collectArgs(event)
        }
        return SlashMeta(WarnRemoveCommand::class.java, args)
    }
}