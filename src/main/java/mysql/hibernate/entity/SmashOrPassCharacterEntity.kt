package mysql.hibernate.entity

import core.utils.TimeUtil
import modules.anilist.AnilistCharacter
import mysql.hibernate.EntityManagerWrapper
import mysql.hibernate.template.HibernateEntity
import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@Entity(name = "SmashOrPassCharacter")
class SmashOrPassCharacterEntity() : HibernateEntity() {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    var year = 0
    var week = 0
    var index = 0
    var name: String = ""
    var characterUrl: String = ""
    var imageUrl: String? = null
    var mediaName: String? = null
    var mediaUrl: String? = null
    var age: String = ""
    var gender: String? = null
    var favourites = 0

    @ElementCollection
    var smashUserIds: MutableSet<Long> = mutableSetOf()

    @ElementCollection
    var passUserIds: MutableSet<Long> = mutableSetOf()


    constructor(anilistCharacter: AnilistCharacter, index: Int) : this() {
        this.year = TimeUtil.getCurrentYear()
        this.week = TimeUtil.getCurrentWeekOfYear()
        this.name = anilistCharacter.name
        this.characterUrl = anilistCharacter.characterUrl
        this.imageUrl = anilistCharacter.imageUrl
        this.mediaName = anilistCharacter.mediaName
        this.mediaUrl = anilistCharacter.mediaUrl
        this.age = anilistCharacter.age
        this.gender = anilistCharacter.gender
        this.favourites = anilistCharacter.favourites
        this.index = index
    }

    companion object {

        @JvmStatic
        fun findAll(entityManager: EntityManagerWrapper): Map<Int, SmashOrPassCharacterEntity> {
            return entityManager.createQuery("FROM SmashOrPassCharacter WHERE year = :year AND week = :week", SmashOrPassCharacterEntity::class.java)
                .setParameter("year", Calendar.getInstance().get(Calendar.YEAR))
                .setParameter("week", TimeUtil.getCurrentWeekOfYear())
                .resultList
                .map {
                    it.entityManager = entityManager
                    return@map it
                }
                .associateBy { it.index }
        }

    }

}