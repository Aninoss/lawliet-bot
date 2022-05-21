package commands.slashadapters

import commands.Category
import commands.Command
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Slash(
    val command: KClass<out Command> = Command::class,
    val name: String = "",
    val description: String = "",
    val commandCategories: Array<Category> = [],
)