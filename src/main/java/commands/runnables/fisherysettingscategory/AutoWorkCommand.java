package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import constants.Settings;
import core.EmbedFactory;
import core.TextManager;
import core.cache.PatreonCache;
import mysql.modules.autowork.DBAutoWork;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;

@CommandProperties(
        trigger = "autowork",
        emoji = "\uD83E\uDD16",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class AutoWorkCommand extends CommandOnOffSwitchAbstract {

    public AutoWorkCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

    @Override
    protected boolean isActive(Member member) {
        return DBAutoWork.getInstance().retrieve().isActive(member.getIdLong());
    }

    @Override
    protected boolean setActive(Member member, boolean active) {
        if (!active || PatreonCache.getInstance().hasPremium(member.getIdLong(), false)) {
            DBAutoWork.getInstance().retrieve().setActive(member.getIdLong(), active);
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected EmbedBuilder generateErrorEmbed() {
        setComponents(EmbedFactory.getPatreonBlockButtons(getLocale()));
        return EmbedFactory.getEmbedDefault(this, getString("error"))
                .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "patreon_title"))
                .setColor(Settings.PREMIUM_COLOR);
    }

}
