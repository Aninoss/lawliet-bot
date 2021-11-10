package commands.slashadapters

import commands.Command
import commands.CommandContainer
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import java.util.*
import kotlin.reflect.KClass

abstract class SlashAdapter {

    protected abstract fun addOptions(commandData: CommandData): CommandData

    abstract fun process(event: SlashCommandEvent): SlashMeta

    fun name(): String {
        val slash = javaClass.getAnnotation(Slash::class.java)
        var name = slash.name
        if (name.isEmpty()) {
            name = Command.getCommandProperties(slash.command).trigger
        }
        return name
    }

    fun description(): String {
        val slash = javaClass.getAnnotation(Slash::class.java)
        var description = slash.description
        if (description.isEmpty()) {
            val trigger = name()
            val clazz = CommandContainer.getCommandMap()[trigger]!!
            val category = Command.getCategory(clazz)
            description = TextManager.getString(Language.EN.locale, category, trigger + "_description")
        }
        return description
    }

    fun commandClass(): KClass<out Command> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.command
    }

    fun generateCommandData(): CommandData {
        val commandData = CommandData(name(), description())
        return addOptions(commandData)
    }

    companion object {

        @JvmStatic
        protected fun collectArgs(event: SlashCommandEvent, vararg exceptions: String): String {
            val argsBuilder = StringBuilder()
            for (option in event.options) {
                if (Arrays.stream(exceptions).noneMatch { exception: String -> option.name == exception }) {
                    if (option.type == OptionType.BOOLEAN && option.asBoolean) {
                        argsBuilder.append(option.name).append(" ")
                    } else {
                        argsBuilder.append(option.asString).append(" ")
                    }
                }
            }
            return argsBuilder.toString()
        }

    }
}