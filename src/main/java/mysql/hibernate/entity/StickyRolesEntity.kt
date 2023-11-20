package mysql.hibernate.entity

import core.atomicassets.AtomicRole
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.ElementCollection
import javax.persistence.Embeddable

const val STICKY_ROLES = "stickyRoles"

@Embeddable
class StickyRolesEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    @ElementCollection
    var roleIds: MutableList<Long> = mutableListOf()
    val roles: MutableList<AtomicRole>
        get() = getAtomicRoleList(roleIds)

    @ElementCollection
    var activeRoles: MutableSet<StickyRolesActiveRoleEntity> = mutableSetOf()
    fun getActiveRoleIdsForMember(memberId: Long): Set<Long> {
        return activeRoles
                .filter { it.memberId == memberId }
                .map { it.roleId }
                .toSet()
    }
    fun addActiveRoleIdsForMember(memberId: Long, roleIds: Set<Long>) {
        if (roleIds.isEmpty()) {
            return
        }
        activeRoles.addAll(roleIds.map { StickyRolesActiveRoleEntity(memberId, it) })
    }
    fun removeActiveRoleIdsForMember(memberId: Long, roleIds: Set<Long>) {
        if (roleIds.isEmpty()) {
            return
        }
        activeRoles.removeIf { it.memberId == memberId && roleIds.contains(it.roleId) }
    }


    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }

}
