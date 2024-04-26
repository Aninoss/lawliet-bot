package mysql.hibernate.entity.assets

import core.collectionadapters.ListAdapter

interface CdnImageListAsset {

    var imageFilenames: MutableList<String>
    val imageUrls: MutableList<String>
        get() = ListAdapter(imageFilenames, { "https://lawlietbot.xyz/cdn/${getFileDir()}/$it" }, { it.split("/")[5] })

    fun getFileDir(): String

}