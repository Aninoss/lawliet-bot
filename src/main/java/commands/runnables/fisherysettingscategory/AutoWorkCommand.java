package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import core.cache.PatreonCache;
import mysql.modules.autowork.DBAutoWork;

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
    protected boolean isActive() {
        return DBAutoWork.getInstance().retrieve().isActive(getMemberId().get());
    }

    @Override
    protected boolean setActive(boolean active) {
        if (!active || PatreonCache.getInstance().getUserTier(getMemberId().get(), false) >= 2) {
            DBAutoWork.getInstance().retrieve().setActive(getMemberId().get(), active);
            return true;
        } else {
            return false;
        }
    }

}
