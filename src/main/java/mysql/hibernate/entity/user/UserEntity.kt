package mysql.hibernate.entity.user

import core.assets.UserAsset
import modules.fishery.Stock
import mysql.hibernate.entity.ReminderEntity
import mysql.hibernate.template.HibernateEntity
import java.time.Duration
import java.time.Instant
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

    var translateTargetLanguageCode: String? = null
    var translateFormal: Boolean? = null

    @Column(name = "smashOrPassYear")
    private var _smashOrPassYear: Int? = null
    var smashOrPassYear: Int
        get() = _smashOrPassYear ?: 0
        set(value) {
            _smashOrPassYear = value
        }

    @Column(name = "smashOrPassWeek")
    private var _smashOrPassWeek: Int? = null
    var smashOrPassWeek: Int
        get() = _smashOrPassWeek ?: 0
        set(value) {
            _smashOrPassWeek = value
        }

    @Column(name = "smashOrPassIndex")
    private var _smashOrPassIndex: Int? = null
    var smashOrPassIndex: Int
        get() = _smashOrPassIndex ?: 0
        set(value) {
            _smashOrPassIndex = value
        }

    @ElementCollection
    val personalNSFWFilter = mutableListOf<String>()

    @ElementCollection
    @Enumerated(EnumType.STRING)
    val autoStocksBuyOrders = mutableMapOf<Stock, AutoStockOrderEntity>()

    @ElementCollection
    @Enumerated(EnumType.STRING)
    val autoStocksSellOrders = mutableMapOf<Stock, AutoStockOrderEntity>()

    @ElementCollection
    var autoStockActivities = mutableListOf<AutoStockActivityEntity>()

    @Enumerated(EnumType.STRING)
    @Column(name = "rolePlayGender")
    private var _rolePlayGender: RolePlayGender? = null
    var rolePlayGender: RolePlayGender
        get() = _rolePlayGender ?: RolePlayGender.ANY
        set(value) {
            _rolePlayGender = value
        }


    constructor() : this("0")

    override fun getUserId(): Long {
        return userId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        txt2img.postLoad(this)
        fisheryDmReminders.values.forEach { it.postLoad(this) }
        rolePlayBlock.postLoad(this)
        autoStocksBuyOrders.values.forEach { it.postLoad(this) }
        autoStocksSellOrders.values.forEach { it.postLoad(this) }
        autoStocksBuyOrders.values.forEach { it.postLoad(this) }
        autoStocksSellOrders.values.forEach { it.postLoad(this) }
    }

    fun hasOutdatedAutoStockActivities(): Boolean {
        return autoStockActivities.any { it.instant.isBefore(Instant.now().minus(Duration.ofDays(7))) }
    }

    fun cleanAutoStockActivities() {
        autoStockActivities.removeIf { it.instant.isBefore(Instant.now().minus(Duration.ofDays(7))) }
    }

    fun rolePlayGenderIsUnspecified(): Boolean {
        return _rolePlayGender == null
    }

}
