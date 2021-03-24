package commands.runnables.fisherysettingscategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import mysql.modules.autoclaim.DBAutoClaim;

@CommandProperties(
        trigger = "autoclaim",
        emoji = "\uD83E\uDD16",
        patreonRequired = true,
        executableWithoutArgs = true
)
//TODO: merge with L.claim
public class AutoClaimCommand extends CommandOnOffSwitchAbstract {

    public AutoClaimCommand(Locale locale, String prefix) {
        super(locale, prefix, true);
    }

    @Override
    protected boolean isActive() {
        return DBAutoClaim.getInstance().retrieve().isActive(getGuildId().get(), getMemberId().get());
    }

    @Override
    protected void setActive(boolean active) {
        DBAutoClaim.getInstance().retrieve().setActive(getMemberId().get(), active);
    }

}
