package mysql.hibernate.entity

import core.atomicassets.AtomicGuildMessageChannel
import core.atomicassets.AtomicRole
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.GenericGenerator
import javax.persistence.*


@Entity(name = "ReactionRole")
class ReactionRoleEntity : HibernateEntity(), HibernateDiscordInterface {

    enum class ComponentType { REACTIONS, BUTTONS, SELECT_MENU }

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    var messageGuildId: Long = 0L

    var messageChannelId: Long = 0L
    val messageChannel: AtomicGuildMessageChannel
        get() = getAtomicGuildMessageChannel(messageChannelId)

    var messageId: Long = 0L

    var title: String = ""

    var description: String? = null

    var imageFilename: String? = null
    var imageUrl: String?
        get() = if (imageFilename != null) "https://lawlietbot.xyz/cdn/reactionroles/$imageFilename" else null
        set(value) {
            if (value == null || value.isEmpty()) {
                imageFilename = null
            } else {
                imageFilename = value.split("/")[5]
            }
        }

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    var slots: MutableList<ReactionRoleSlotEntity> = mutableListOf()

    @ElementCollection
    var roleRequirementIds: MutableList<Long> = mutableListOf()
    val roleRequirements: MutableList<AtomicRole>
        get() = getAtomicRoleList(roleRequirementIds)

    var roleRemovals: Boolean = true

    var multipleSlots: Boolean = true

    var slotOverview: Boolean = false

    @Enumerated(EnumType.STRING)
    var componentType: ComponentType = ComponentType.BUTTONS

    var roleCounters: Boolean = false

    var newGeneration: Boolean = true


    override fun getGuildId(): Long {
        return messageGuildId
    }

    fun copy(): ReactionRoleEntity {
        val copy = ReactionRoleEntity()
        copy.messageGuildId = messageGuildId
        copy.messageChannelId = messageChannelId
        copy.messageId = messageId
        copy.title = title
        copy.description = description
        copy.imageFilename = imageFilename
        copy.slots = ArrayList(slots.map { it.copy() })
        copy.roleRequirementIds = ArrayList(roleRequirementIds)
        copy.roleRemovals = roleRemovals
        copy.multipleSlots = multipleSlots
        copy.slotOverview = slotOverview
        copy.componentType = componentType
        copy.roleCounters = roleCounters
        return copy
    }

}