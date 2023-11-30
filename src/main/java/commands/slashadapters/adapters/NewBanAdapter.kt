package commands.slashadapters.adapters

import commands.runnables.moderationcategory.NewBanCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = NewBanCommand::class)
class NewBanAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "time", "moderation_joinspan", true),
            generateOptionData(OptionType.STRING, "reason", "moderation_reason", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(NewBanCommand::class.java, collectArgs(event))
    }

}