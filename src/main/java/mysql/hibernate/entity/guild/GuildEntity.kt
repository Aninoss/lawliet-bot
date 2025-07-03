package mysql.hibernate.entity.guild

import constants.Language
import core.assets.GuildAsset
import core.cache.ServerPatreonBoostCache
import mysql.hibernate.entity.CustomRolePlayEntity
import mysql.hibernate.entity.GiveawayEntity
import mysql.hibernate.entity.ReactionRoleEntity
import mysql.hibernate.entity.ReminderEntity
import mysql.hibernate.entity.assets.LanguageAsset
import mysql.hibernate.entity.guild.welcomemessages.WELCOME_MESSAGES
import mysql.hibernate.entity.guild.welcomemessages.WelcomeMessagesEntity
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.NotFound
import org.hibernate.annotations.NotFoundAction
import org.hibernate.annotations.SortNatural
import java.time.LocalDate
import javax.persistence.*


@Entity(name = "Guild")
class GuildEntity(key: String) : HibernateEntity(), GuildAsset, LanguageAsset {

    @Id
    private val guildId = key

    @Column(name = "prefix")
    private var _prefix: String? = null
    var prefix: String
        get() = _prefix ?: "L."
        set(value) {
            _prefix = value
        }

    @Column(name = "language")
    @Enumerated(EnumType.STRING)
    private var _language: Language? = null
    override var language: Language
        get() = _language ?: Language.EN
        set(value) {
            _language = value
        }

    @Column(name = "removeAuthorMessage")
    private var _removeAuthorMessage: Boolean? = null
    var removeAuthorMessage: Boolean
        get() = _removeAuthorMessage ?: false
        set(value) {
            _removeAuthorMessage = value
        }
    val removeAuthorMessageEffectively: Boolean
        get() = removeAuthorMessage && ServerPatreonBoostCache.get(guildId.toLong())

    @Column(name = "txt2imgBanned")
    private var _txt2imgBanned: Boolean? = null
    var txt2imgBanned: Boolean
        get() = _txt2imgBanned ?: false
        set(value) {
            _txt2imgBanned = value
        }

    var latestPresentDate: LocalDate? = LocalDate.now()

    var apiToken: String? = null
    val apiTokenEffectively: String?
        get() = if (ServerPatreonBoostCache.get(guildId.toLong())) apiToken else null

    @Embedded
    @Column(name = FISHERY)
    val fishery = FisheryEntity()

    @Embedded
    @Column(name = MODERATION)
    val moderation = ModerationEntity()

    @Embedded
    @Column(name = INVITE_FILTER)
    val inviteFilter = InviteFilterEntity()

    @Embedded
    @Column(name = WORD_FILTER)
    val wordFilter = WordFilterEntity()

    @Embedded
    @Column(name = STICKY_ROLES)
    val stickyRoles = StickyRolesEntity()

    @Embedded
    @Column(name = TICKETS)
    val tickets = TicketsEntity()

    @Embedded
    @Column(name = WELCOME_MESSAGES)
    val welcomeMessages = WelcomeMessagesEntity()

    @Embedded
    @Column(name = BIRTHDAY)
    val birthday = BirthdayEntity()

    @Embedded
    @Column(name = AUTO_CHANNEL)
    val autoChannel = AutoChannelEntity()

    @ElementCollection
    @SortNatural
    val customCommands = sortedMapOf<String, CustomCommandEntity>()

    @ElementCollection
    val commandChannelShortcuts = mutableMapOf<Long, String>()

    @ElementCollection
    val disabledCommandsAndCategories = mutableSetOf<String>()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotFound(action = NotFoundAction.IGNORE)
    var reactionRoles: Map<Long, ReactionRoleEntity> = mutableMapOf()

    val reminders: List<ReminderEntity>
        get() = entityManager.findAllWithValue(ReminderEntity::class.java, "targetId", guildId.toLong())

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotFound(action = NotFoundAction.IGNORE)
    val customRolePlayCommands: MutableMap<String, CustomRolePlayEntity> = sortedMapOf()
    val customRolePlayCommandsEffectively: Map<String, CustomRolePlayEntity>
        get() = if (ServerPatreonBoostCache.get(guildId.toLong())) customRolePlayCommands else mapOf()

    @ElementCollection
    val slashPermissions = mutableListOf<SlashPermissionEntity>()

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @NotFound(action = NotFoundAction.IGNORE)
    var giveaways: MutableMap<Long, GiveawayEntity> = mutableMapOf()

    @ElementCollection
    val whitelistedChannelIds = mutableListOf<Long>()

    constructor() : this("0")

    override fun getGuildId(): Long {
        return guildId.toLong()
    }

    @PostLoad
    override fun postLoad() {
        fishery.postLoad(this)
        moderation.postLoad(this)
        inviteFilter.postLoad(this)
        wordFilter.postLoad(this)
        stickyRoles.postLoad(this)
        tickets.postLoad(this)
        welcomeMessages.postLoad(this)
        birthday.postLoad(this)
        autoChannel.postLoad(this)
    }

    override fun postRemove() {
        entityManager.deleteAllWithValue(ReminderEntity::class.java, "targetId", guildId.toLong())
    }

}
