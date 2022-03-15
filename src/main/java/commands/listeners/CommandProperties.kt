package commands.listeners

import net.dv8tion.jda.api.Permission

@Retention(AnnotationRetention.RUNTIME)
annotation class CommandProperties(
    val trigger: String,
    val aliases: Array<String> = [],
    val emoji: String,
    val nsfw: Boolean = false,
    val executableWithoutArgs: Boolean,
    val deleteOnTimeOut: Boolean = false,
    val botChannelPermissions: Array<Permission> = [],
    val botGuildPermissions: Array<Permission> = [],
    val userChannelPermissions: Array<Permission> = [],
    val userGuildPermissions: Array<Permission> = [],
    val requiresEmbeds: Boolean = true,
    val maxCalculationTimeSec: Int = 30,
    val patreonRequired: Boolean = false,
    val exclusiveGuilds: LongArray = [],
    val exclusiveUsers: LongArray = [],
    val turnOffTimeout: Boolean = false,
    val releaseDate: IntArray = [],
    val onlyPublicVersion: Boolean = false,
    val usesExtEmotes: Boolean = false,
    val requiresFullMemberCache: Boolean = false
)