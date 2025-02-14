package mysql.hibernate.entity.assets

import core.LocalFile
import core.collectionadapters.ListAdapter

interface CdnImageListAsset {

    var imageFilenames: MutableList<String>
    val imageUrls: MutableList<String>
        get() = ListAdapter(imageFilenames, { "https://lawlietbot.xyz/cdn/${getFileDir()}/$it" }, { it.split("/")[5] })
    val imageFiles: MutableList<LocalFile>
        get() = ListAdapter(imageFilenames, { LocalFile(LocalFile.Directory.CDN, "${getFileDir()}/$it") }, { it.cdnGetUrl().split("/")[5] })

    fun getFileDir(): String

    fun urlToFile(url: String): LocalFile {
        val filename = url.split("/")[5]
        return LocalFile(LocalFile.Directory.CDN, "${getFileDir()}/$filename")
    }

}