package commands.slashadapters

import commands.Command
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class Slash(
    val name: String = "",
    val command: KClass<out Command> = Command::class,
    val description: String = ""
)