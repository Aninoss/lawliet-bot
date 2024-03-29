package commands.runnables;

import java.util.Locale;
import commands.CommandEvent;
import org.jetbrains.annotations.NotNull;

public abstract class FisheryMemberAccountInterface extends MemberAccountAbstract implements FisheryInterface {

    public FisheryMemberAccountInterface(Locale locale, String prefix) {
        super(locale, prefix, false, false, false);
    }

    public FisheryMemberAccountInterface(Locale locale, String prefix, boolean includeNotInGuild, boolean requireMemberMention, boolean allowBots) {
        super(locale, prefix, includeNotInGuild, requireMemberMention, allowBots);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        return onFisheryTrigger(event, args);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        return super.onTrigger(event, args);
    }

}
