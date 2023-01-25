package commands.slashadapters

import commands.Category
import commands.Command
import commands.CommandContainer
import constants.Language
import core.TextManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import okhttp3.internal.toImmutableList
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.jvmName

abstract class SlashAdapter {

    protected abstract fun addOptions(commandData: SlashCommandData): SlashCommandData

    abstract fun process(event: SlashCommandInteractionEvent): SlashMeta

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

    fun requiredPermissions(): Collection<Permission> {
        val permissions = ArrayList<Permission>()
        val slash = javaClass.getAnnotation(Slash::class.java)
        permissions += slash.permissions
        if (slash.command != Command::class) {
            val commandProperties = Command.getCommandProperties(slash.command)
            permissions += commandProperties.userGuildPermissions
            permissions += commandProperties.userChannelPermissions
        }
        return permissions.distinct()
    }

    fun commandClass(): KClass<out Command> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.command
    }

    fun commandAssociations(): Array<KClass<out Command>> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.commandAssociations
    }

    fun commandAssociationCategories(): Array<Category> {
        val slash = javaClass.getAnnotation(Slash::class.java)
        return slash.commandAssociationCategories
    }

    fun messageCommandAssociations(): List<String> {
        val list = ArrayList<String>()

        val commandClass = commandClass()
        if (!commandClass.jvmName.equals(Command::class.jvmName)) {
            val trigger = Command.getCommandProperties(commandClass).trigger
            list += trigger
        }
        list += commandAssociations()
            .map { Command.getCommandProperties(it).trigger }
        list += commandAssociationCategories()
            .map { it.id }

        return list.toImmutableList()
    }

    fun nsfw(): Boolean {
        val slash = javaClass.getAnnotation(Slash::class.java)
        if (slash.command != Command::class) {
            return Command.getCommandProperties(slash.command).nsfw
        }
        return slash.nsfw
    }

    fun generateCommandData(): SlashCommandData {
        val commandData = Commands.slash(name(), description())
        commandData.isGuildOnly = true
        commandData.defaultPermissions = DefaultMemberPermissions.enabledFor(requiredPermissions())
        commandData.isNSFW = nsfw()
        return addOptions(commandData)
    }

    open fun retrieveChoices(event: CommandAutoCompleteInteractionEvent): List<net.dv8tion.jda.api.interactions.commands.Command.Choice> {
        return emptyList()
    }

    companion object {

        @JvmStatic
        protected fun collectArgs(event: SlashCommandInteractionEvent, vararg exceptions: String): String {
            val argsBuilder = StringBuilder()
            for (option in event.options) {
                if (Arrays.stream(exceptions).noneMatch { exception: String -> option.name == exception }) {
                    when (option.type) {
                        OptionType.BOOLEAN -> argsBuilder.append(option.name).append(" ")
                        OptionType.ATTACHMENT -> argsBuilder.append(option.asAttachment.url).append(" ")
                        else -> argsBuilder.append(option.asString).append(" ")
                    }
                }
            }
            return argsBuilder.toString()
        }

    }
}