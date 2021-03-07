package commands.runnables;

import java.util.Locale;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class FisheryMemberAccountInterface extends MemberAccountAbstract implements FisheryInterface {

    public FisheryMemberAccountInterface(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        return ((FisheryInterface) this).onTrigger(event, args);
    }

    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        return ((MemberAccountAbstract) this).onTrigger(event, args);
    }

}
