package dashboard

import net.dv8tion.jda.api.Permission

@Retention(AnnotationRetention.RUNTIME)
annotation class DashboardProperties(
    val id: String,
    val botPermissions: Array<Permission> = [],
    val userPermissions: Array<Permission> = [],
)