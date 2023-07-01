package dashboard.components

import commands.Command
import commands.CommandManager
import core.CustomObservableList
import core.MemberCacheController
import core.ShardManager
import core.atomicassets.AtomicTextChannel
import dashboard.ActionResult
import dashboard.component.DashboardComboBox
import dashboard.data.DiscordEntity
import java.util.*
import kotlin.reflect.KClass

class DashboardMultiTextChannelsComboBox(label: String, locale: Locale, guildId: Long, val selectedChannels: CustomObservableList<Long>,
                                         canBeEmpty: Boolean, max: Int, memberId: Long? = null, commandAccessRequirement: KClass<out Command>? = null
) : DashboardComboBox(label, DataType.TEXT_CHANNELS, canBeEmpty, max) {

    init {
        selectedValues = selectedChannels.map {
            val atomicChannel = AtomicTextChannel(guildId, it)
            DiscordEntity(it.toString(), atomicChannel.getPrefixedName(locale))
        }
        setActionListener {
            if (commandAccessRequirement != null && memberId != null) {
                val guild = ShardManager.getLocalGuildById(guildId).get()
                val member = MemberCacheController.getInstance().loadMember(guild, memberId).get()
                if (!CommandManager.commandIsTurnedOnEffectively(commandAccessRequirement.java, member, null)) {
                    return@setActionListener ActionResult()
                        .withRedraw()
                }
            }

            if (it.type == "add") {
                selectedChannels.add(it.data.toLong())
            } else if (it.type == "remove") {
                selectedChannels.remove(it.data.toLong())
            }
            ActionResult()
        }
    }

}