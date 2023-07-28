package mysql.hibernate.template

import core.assets.GuildAsset
import core.atomicassets.AtomicTextChannel

interface HibernateDiscordInterface : GuildAsset {

    fun getAtomicTextChannel(channelId: Long?): AtomicTextChannel {
        return AtomicTextChannel(guildId, channelId ?: 0L)
    }

}
