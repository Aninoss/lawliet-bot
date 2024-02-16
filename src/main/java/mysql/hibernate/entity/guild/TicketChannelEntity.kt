package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class TicketChannelEntity(
        var channelId: Long = 0L,
        var memberId: Long = 0L,
        var logChannelId: Long = 0L,
        var logMessageId: Long = 0L,
        var assigned: Boolean = false,
        var introductionMessageId: Long = 0L,
        var assignmentMode: TicketsEntity.AssignmentMode = TicketsEntity.AssignmentMode.MANUAL
)