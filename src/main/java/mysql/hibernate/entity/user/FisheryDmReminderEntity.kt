package mysql.hibernate.entity.user

import commands.Category
import constants.Language
import core.Program
import core.ShardManager
import core.TextManager
import core.assets.UserAsset
import core.cache.UserBannedCache
import core.utils.ComponentsUtil
import core.utils.JDAUtil
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.entity.assets.LanguageAsset
import mysql.hibernate.template.HibernateEmbeddedEntity
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.buttons.ButtonStyle
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
class FisheryDmReminderEntity(
        @Enumerated(EnumType.STRING) var type: Type = Type.values()[0],
        @Enumerated(EnumType.STRING) override var language: Language = Language.EN
) : HibernateEmbeddedEntity<UserEntity>(), UserAsset, LanguageAsset {

    enum class Type {
        DAILY, WORK, CLAIM, SURVEY
    }

    var errors: Int = 0
    var dmsOpenForPublicInstance = true

    fun sendReminder(title: String, description: String, vararg buttons: Button?) {
        if ((!Program.publicInstance() && dmsOpenForPublicInstance) ||
                UserBannedCache.getInstance().isBanned(userId)
        ) {
            return
        }

        try {
            val disableButton = Button.of(
                    ButtonStyle.SECONDARY,
                    DISABLE_BUTTON_ID_PREFIX + type.name,
                    TextManager.getString(locale, Category.FISHERY, "dm_reminder_disable_button")
            )
            val rowButtons = buttons.filterNotNull() + disableButton

            val components = ArrayList<ContainerChildComponent>()
            components.add(TextDisplay.of(description))
            components.add(TextDisplay.of("-# " + TextManager.getString(locale, Category.FISHERY, "cooldowns_footer")))
            components.add(ActionRow.of(rowButtons))
            val componentTree = ComponentsUtil.createCommandComponentTree(title, components, ComponentsUtil.DEFAULT_CONTAINER_COLOR)

            val channel = JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId).complete()
            channel.sendMessageComponents(componentTree).useComponentsV2().complete()

            if (errors > 0 || !dmsOpenForPublicInstance) {
                beginTransaction()
                if (Program.publicInstance()) {
                    dmsOpenForPublicInstance = true
                }
                errors = 0
                commitTransaction()
            }
        } catch (e: Throwable) {
            beginTransaction()
            if (Program.publicInstance()) {
                dmsOpenForPublicInstance = false
            }
            if (++errors >= 4) {
                hibernateEntity.fisheryDmReminders.remove(type)
            }
            commitTransaction()
        }
    }


    override fun getUserId(): Long {
        return hibernateEntity.userId
    }

    companion object {

        const val DISABLE_BUTTON_ID_PREFIX = "fishery_dm_disable:"

        @JvmStatic
        fun findAllUserEntitiesWithType(entityManager: EntityManagerWrapper, type: Type): List<UserEntity> {
            return entityManager.createNativeQuery("{'fisheryDmReminders." + type.name + "': {\$exists: true}}", UserEntity::class.java).getResultList()
                    .map {
                        val userEntity = it as UserEntity
                        userEntity.entityManager = entityManager
                        return@map userEntity
                    }
                    .filter { Program.publicInstance() || ShardManager.getCachedUserById(it.userId).isPresent }
        }

    }

}
