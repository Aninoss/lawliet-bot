package mysql.hibernate.entity.guild

import core.atomicassets.AtomicRole
import core.atomicassets.AtomicTextChannel
import core.cache.ServerPatreonBoostCache
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.*

const val TICKETS = "tickets"

@Embeddable
class TicketsEntity : HibernateEmbeddedEntity<GuildEntity>(), HibernateDiscordInterface {

    enum class AssignmentMode { FIRST, EVERYONE, MANUAL }

    var logChannelId: Long? = null
    val logChannel: AtomicTextChannel
        get() = getAtomicTextChannel(logChannelId)

    @ElementCollection
    var staffRoleIds = mutableListOf<Long>()
    val staffRoles: MutableList<AtomicRole>
        get() = getAtomicRoleList(staffRoleIds)

    @Column(name = "$TICKETS.assignmentMode")
    @Enumerated(EnumType.STRING)
    var _assignmentMode: AssignmentMode? = null
    var assignmentMode: AssignmentMode
        get() = _assignmentMode ?: AssignmentMode.MANUAL
        set(value) {
            _assignmentMode = value
        }

    var autoCloseHours: Int? = null
    val autoCloseHoursEffectively: Int?
        get() = if (ServerPatreonBoostCache.get(guildId)) autoCloseHours else null

    var greetingText: String? = null

    @Column(name = "$TICKETS.pingStaffRoles")
    var _pingStaffRoles: Boolean? = null
    var pingStaffRoles: Boolean
        get() = _pingStaffRoles ?: true
        set(value) {
            _pingStaffRoles = value
        }

    @Column(name = "$TICKETS.enforceModal")
    var _enforceModal: Boolean? = null
    var enforceModal: Boolean
        get() = _enforceModal ?: true
        set(value) {
            _enforceModal = value
        }

    @Column(name = "$TICKETS.membersCanCloseTickets")
    var _membersCanCloseTickets: Boolean? = null
    var membersCanCloseTickets: Boolean
        get() = _membersCanCloseTickets ?: true
        set(value) {
            _membersCanCloseTickets = value
        }

    @Column(name = "$TICKETS.protocols")
    var _protocols: Boolean? = null
    var protocols: Boolean
        get() = _protocols ?: false
        set(value) {
            _protocols = value
        }
    val protocolsEffectively: Boolean
        get() = if (ServerPatreonBoostCache.get(guildId)) protocols else false

    @Column(name = "$TICKETS.deleteChannelsOnClose")
    var _deleteChannelsOnClose: Boolean? = null
    var deleteChannelsOnClose: Boolean
        get() = _deleteChannelsOnClose ?: true
        set(value) {
            _deleteChannelsOnClose = value
        }

    @Column(name = "$TICKETS.ticketIndex")
    var _ticketIndex: Int? = null
    var ticketIndex: Int
        get() = _ticketIndex ?: 0
        set(value) {
            _ticketIndex = value
        }

    @ElementCollection
    var ticketChannels = mutableMapOf<Long, TicketChannelEntity>()


    override fun getGuildId(): Long {
        return hibernateEntity.guildId
    }


    companion object {

        @JvmStatic
        fun findAllGuildEntitiesWithTicketsAutoClose(entityManager: EntityManagerWrapper): List<GuildEntity> {
            return entityManager.createNativeQuery("{'tickets.autoCloseHours': {\$exists: true}}", GuildEntity::class.java).getResultList()
                    .map {
                        val guildEntity = it as GuildEntity
                        guildEntity.entityManager = entityManager
                        return@map guildEntity
                    }
        }

    }

}
