package mysql.hibernate.entity

import javax.persistence.Embeddable

@Embeddable
class StickyRolesActiveRoleEntity(
        var memberId: Long = 0L,
        var roleId: Long = 0L
)