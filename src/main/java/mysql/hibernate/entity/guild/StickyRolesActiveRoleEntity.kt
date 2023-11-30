package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class StickyRolesActiveRoleEntity(
        var memberId: Long = 0L,
        var roleId: Long = 0L
)