package mysql.hibernate.entity

import org.hibernate.annotations.GenericGenerator
import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id


@Entity(name = "InviteTrackingSlot")
class InviteTrackingSlotEntity(
    var inviterUserId: Long = 0L,
    var invitedDate: LocalDate = LocalDate.MIN,
    var lastMessageDate: LocalDate = LocalDate.MIN,
    var fakeInvite: Boolean = false
) {

    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    private var id: String? = null

    fun isRetained(): Boolean {
        return isActive() || !lastMessageDate.isBefore(invitedDate.plusDays(7))
    }

    fun isActive(): Boolean {
        return !LocalDate.now().isAfter(lastMessageDate.plusDays(7))
    }

}