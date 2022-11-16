package dashboard

import commands.Command
import net.dv8tion.jda.api.Permission
import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class DashboardProperties(
    val id: String,
    val botPermissions: Array<Permission> = [],
    val userPermissions: Array<Permission> = [],
    val commandAccessRequirements: Array<KClass<out Command>> = []
)