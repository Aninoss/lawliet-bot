package dashboard.pages

import core.TextManager
import core.patreon.PatreonCache
import dashboard.ActionResult
import dashboard.DashboardCategory
import dashboard.DashboardComponent
import dashboard.DashboardProperties
import dashboard.component.DashboardButton
import dashboard.component.DashboardTextField
import dashboard.container.HorizontalContainer
import dashboard.container.VerticalContainer
import mysql.hibernate.entity.BotLogEntity
import mysql.hibernate.entity.guild.GuildEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import java.util.*

@DashboardProperties(
        id = "customizations",
        userPermissions = [Permission.MANAGE_SERVER]
)
class CustomizationsCategory(guildId: Long, userId: Long, locale: Locale, guildEntity: GuildEntity) : DashboardCategory(guildId, userId, locale, guildEntity) {

    override fun retrievePageTitle(): String {
        return getString(TextManager.GENERAL, "dashboard_customizations")
    }

    override fun generateComponents(guild: Guild, mainContainer: VerticalContainer) {
        mainContainer.isCard = true
        mainContainer.add(generateUsernameComponents(guild))
    }

    fun generateUsernameComponents(guild: Guild): DashboardComponent {
        val horizontalContainer = HorizontalContainer()
        horizontalContainer.allowWrap = true
        horizontalContainer.alignment = HorizontalContainer.Alignment.BOTTOM

        val usernameTextField = DashboardTextField(getString(TextManager.GENERAL, "dashboard_customizations_username"), 1, Member.MAX_NICKNAME_LENGTH) {
            val previousName = guild.selfMember.effectiveName
            guild.selfMember.manager.setNickname(it.data).complete()
            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOMIZATIONS_USERNAME, atomicMember, previousName, guild.selfMember.effectiveName)
            guildEntity.commitTransaction()
            ActionResult()
        }
        usernameTextField.value = guild.selfMember.effectiveName
        usernameTextField.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        horizontalContainer.add(usernameTextField)

        val usernameResetButton = DashboardButton(getString(TextManager.GENERAL, "dashboard_customizations_reset_username")) {
            val previousName = guild.selfMember.effectiveName
            guild.selfMember.manager.setNickname("").complete()
            guildEntity.beginTransaction()
            BotLogEntity.log(entityManager, BotLogEntity.Event.CUSTOMIZATIONS_USERNAME, atomicMember, previousName, guild.selfMember.effectiveName)
            guildEntity.commitTransaction()
            ActionResult()
                .withRedraw()
        }
        usernameResetButton.setCanExpand(false)
        usernameResetButton.isEnabled = PatreonCache.getInstance().isUnlockedPlus(guild.idLong)
        horizontalContainer.add(usernameResetButton)
        return horizontalContainer
    }

}