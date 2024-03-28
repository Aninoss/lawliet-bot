package mysql.hibernate.entity.user

import core.assets.UserAsset
import mysql.hibernate.entity.ReminderEntity
import mysql.hibernate.template.HibernateEntity
import javax.persistence.*


@Entity(name = "User")
class UserEntity(key: String) : HibernateEntity(), UserAsset {

    @Id
    private val userId = key

    var banReason: String? = null

    @Embedded
    @Column(name = TXT2IMG)
    val txt2img = Txt2ImgEntity()

    val reminders: List<ReminderEntity>
        get() = entityManager.findAllWithValue(ReminderEntity::class.java, "targetId", userId.toLong())

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyEnumerated(EnumType.STRING)
    val fisheryDmReminders = mutableMapOf<FisheryDmReminderEntity.Type, FisheryDmReminderEntity>()

    @Embedded
    @Column(name = ROLE_PLAY_BLOCK)
    val rolePlayBlock = RolePlayBlockEntity()


    constructor() : this("0")

    override fun getUserId(): Long {
        return userId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        txt2img.postLoad(this)
        fisheryDmReminders.values.forEach { it.postLoad(this) }
        rolePlayBlock.postLoad(this)
    }

}
