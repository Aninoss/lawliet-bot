package commands.slashadapters.adapters

import commands.runnables.moderationcategory.JailCommand
import commands.slashadapters.Slash
import commands.slashadapters.SlashAdapter
import commands.slashadapters.SlashMeta
import mysql.hibernate.entity.GuildEntity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

@Slash(command = JailCommand::class)
class JailAdapter : SlashAdapter() {

    public override fun addOptions(commandData: SlashCommandData): SlashCommandData {
        return commandData.addOptions(
            generateOptionData(OptionType.STRING, "members", "moderation_members", true),
            generateOptionData(OptionType.STRING, "reason", "moderation_reason", false),
            generateOptionData(OptionType.STRING, "duration", "moderation_duration", false)
        )
    }

    override fun process(event: SlashCommandInteractionEvent, guildEntity: GuildEntity): SlashMeta {
        return SlashMeta(JailCommand::class.java, collectArgs(event))
    }

}