package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import core.cache.PatreonCache;
import mysql.modules.autoclaim.DBAutoClaim;

@CommandProperties(
        trigger = "autoclaim",
        emoji = "\uD83E\uDD16",
        executableWithoutArgs = true
)
public class AutoClaimCommand extends CommandOnOffSwitchAbstract {

    public AutoClaimCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

    @Override
    protected boolean isActive() {
        return DBAutoClaim.getInstance().retrieve().isActive(getGuildId().get(), getMemberId().get());
    }

    @Override
    protected boolean setActive(boolean active) {
        if (!active || PatreonCache.getInstance().getUserTier(getMemberId().get(), false) >= 2) {
            DBAutoClaim.getInstance().retrieve().setActive(getMemberId().get(), active);
            return true;
        } else {
            return false;
        }
    }

}
