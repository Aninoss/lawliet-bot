package mysql.hibernate.entity.guild

import javax.persistence.Embeddable

@Embeddable
class CustomCommandEntity(
        var textResponse: String = ""
)