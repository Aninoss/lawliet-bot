package commands.cooldownchecker

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import constants.Settings
import java.util.concurrent.TimeUnit

object CoolDownManager {

    private val coolDownUserDataMap = CacheBuilder.newBuilder()
        .expireAfterAccess(Settings.COOLDOWN_TIME_SEC.toLong(), TimeUnit.SECONDS)
        .build(object : CacheLoader<Long, CoolDownUserData>() {
            override fun load(userId: Long): CoolDownUserData {
                return CoolDownUserData()
            }
        })

    @JvmStatic
    @Synchronized
    fun getCoolDownData(userId: Long): CoolDownUserData {
        return coolDownUserDataMap.getUnchecked(userId)
    }

}