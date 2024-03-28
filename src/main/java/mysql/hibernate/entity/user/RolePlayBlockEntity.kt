package mysql.hibernate.entity.user

import core.assets.UserAsset
import mysql.hibernate.template.HibernateEmbeddedEntity
import javax.persistence.Column
import javax.persistence.ElementCollection
import javax.persistence.Embeddable

const val ROLE_PLAY_BLOCK = "rolePlayBlock"

@Embeddable
class RolePlayBlockEntity : HibernateEmbeddedEntity<UserEntity>(), UserAsset {

    @Column(name = "$ROLE_PLAY_BLOCK.blockByDefault")
    private var _blockByDefault: Boolean? = null
    var blockByDefault: Boolean
        get() = _blockByDefault ?: false
        set(value) {
            _blockByDefault = value
        }

    @ElementCollection
    var allowedUserIds = mutableSetOf<Long>()

    @ElementCollection
    var blockedUserIds = mutableSetOf<Long>()


    override fun getUserId(): Long {
        return hibernateEntity.userId
    }

}
