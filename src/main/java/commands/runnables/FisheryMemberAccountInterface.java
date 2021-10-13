package commands.runnables;

import java.util.Locale;
import commands.CommandEvent;

public abstract class FisheryMemberAccountInterface extends MemberAccountAbstract implements FisheryInterface {

    public FisheryMemberAccountInterface(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(CommandEvent event, String args) throws Throwable {
        return onFisheryTrigger(event, args);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        return super.onTrigger(event, args);
    }

}
