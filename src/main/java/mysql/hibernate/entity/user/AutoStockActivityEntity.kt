package mysql.hibernate.entity.user

import core.assets.UserAsset
import modules.fishery.Stock
import mysql.hibernate.InstantConverter
import mysql.hibernate.template.HibernateEmbeddedEntity
import java.time.Instant
import javax.persistence.Convert
import javax.persistence.Embeddable
import javax.persistence.EnumType
import javax.persistence.Enumerated

@Embeddable
class AutoStockActivityEntity(
    @Enumerated(EnumType.STRING) var type: Type = Type.entries[0],
    @Enumerated(EnumType.STRING) var stock: Stock = Stock.entries[0],
    var value: Long = 0L,
    var pricePerShare: Long = 0L
) : HibernateEmbeddedEntity<UserEntity>(), UserAsset {

    @Convert(converter = InstantConverter::class)
    var instant: Instant = Instant.now()

    enum class Type { BUY, SELL, BUY_REACTIVATION, SELL_REACTIVATION }

    override fun getUserId(): Long {
        return hibernateEntity.userId
    }

}
