package mysql.hibernate.entity.assets

interface CdnImageAsset {

    var imageFilename: String?
    var imageUrl: String?
        get() = if (imageFilename != null) "https://lawlietbot.xyz/cdn/${getFileDir()}/$imageFilename" else null
        set(value) {
            if (value.isNullOrEmpty()) {
                imageFilename = null
            } else {
                imageFilename = value.split("/")[5]
            }
        }

    fun getFileDir(): String

}