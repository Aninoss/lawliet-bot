package mysql.hibernate.entity.user

import commands.Category
import constants.Language
import core.Program
import core.ShardManager
import core.TextManager
import core.assets.UserAsset
import core.cache.UserBannedCache
import core.components.ActionRows
import core.utils.JDAUtil
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.entity.assets.LanguageAsset
import mysql.hibernate.template.HibernateEmbeddedEntity
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.buttons.Button
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

    fun sendEmbed(eb: EmbedBuilder, vararg buttons: Button?) {
        if ((!Program.publicInstance() && dmsOpenForPublicInstance) ||
                UserBannedCache.getInstance().isBanned(userId)
        ) {
            return
        }

        eb.setFooter(TextManager.getString(locale, Category.FISHERY, "cooldowns_footer"))

        try {
            val channel = JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId).complete()
            channel.sendMessageEmbeds(eb.build()).setComponents(ActionRows.of(*buttons)).complete()

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
