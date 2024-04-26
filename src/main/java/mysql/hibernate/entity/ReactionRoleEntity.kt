package mysql.hibernate.entity

import core.atomicassets.AtomicGuildMessageChannel
import core.atomicassets.AtomicRole
import mysql.hibernate.entity.assets.CdnImageAsset
import mysql.hibernate.template.HibernateDiscordInterface
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.GenericGenerator
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import javax.persistence.*


@Entity(name = "ReactionRole")
class ReactionRoleEntity : HibernateEntity(), HibernateDiscordInterface, CdnImageAsset {

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

    override var imageFilename: String? = null

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotFound(action = NotFoundAction.IGNORE)
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

    override fun getFileDir(): String {
        return "reactionroles"
    }

}