package mysql.hibernate.template

import core.atomicassets.AtomicGuildChannel
import core.atomicassets.AtomicGuildMessageChannel
import core.atomicassets.AtomicMember
import core.atomicassets.AtomicRole
import core.collectionadapters.ListAdapter

interface HibernateDiscordInterface {

    val guildId: Long

    fun getAtomicGuildChannelList(channelIdList: List<Long>): MutableList<AtomicGuildChannel> {
        return ListAdapter(channelIdList, { AtomicGuildChannel(guildId, it) }, { it.idLong })
    }

    fun getAtomicGuildMessageChannel(channelId: Long?): AtomicGuildMessageChannel {
        return AtomicGuildMessageChannel(guildId, channelId ?: 0L)
    }

    fun getAtomicGuildMessageChannelList(channelIdList: List<Long>): MutableList<AtomicGuildMessageChannel> {
        return ListAdapter(channelIdList, { AtomicGuildMessageChannel(guildId, it) }, { it.idLong })
    }

    fun getAtomicMemberList(userIdList: List<Long>): MutableList<AtomicMember> {
        return ListAdapter(userIdList, { AtomicMember(guildId, it) }, { it.idLong })
    }

    fun getAtomicRoleList(roleIdList: List<Long>): MutableList<AtomicRole> {
        return ListAdapter(roleIdList, { AtomicRole(guildId, it) }, { it.idLong })
    }

}
