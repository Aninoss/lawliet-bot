package mysql.hibernate.template

import core.assets.GuildAsset
import core.atomicassets.AtomicRole
import core.atomicassets.AtomicTextChannel
import core.collectionadapters.ListAdapter

interface HibernateDiscordInterface : GuildAsset {

    fun getAtomicTextChannel(channelId: Long?): AtomicTextChannel {
        return AtomicTextChannel(guildId, channelId ?: 0L)
    }

    fun getAtomicTextChannelList(channelIdList: List<Long>): MutableList<AtomicTextChannel> {
        return ListAdapter(channelIdList, { AtomicTextChannel(guildId, it) }, { it.idLong })
    }

    fun getAtomicRoleList(roleIdList: List<Long>): MutableList<AtomicRole> {
        return ListAdapter(roleIdList, { AtomicRole(guildId, it) }, { it.idLong })
    }

}
