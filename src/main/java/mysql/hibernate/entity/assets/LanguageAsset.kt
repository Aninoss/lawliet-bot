package mysql.hibernate.entity.assets

import constants.Language
import java.util.*

interface LanguageAsset {

    var language: Language
    val locale: Locale
        get() = language.locale

}