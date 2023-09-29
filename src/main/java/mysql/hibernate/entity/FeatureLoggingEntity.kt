package mysql.hibernate.entity

import core.featurelogger.PremiumFeature
import mysql.hibernate.template.HibernateEntity
import java.time.LocalDate
import javax.persistence.*


@Entity(name = "FeatureLogging")
class FeatureLoggingEntity(key: LocalDate) : HibernateEntity() {

    @Id
    private val date = key

    var guildHours = 0L

    @ElementCollection
    @MapKeyEnumerated(EnumType.STRING)
    val features = mutableMapOf<PremiumFeature, Int>()


    constructor() : this(LocalDate.MIN)

}