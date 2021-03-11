package commands.runnables;

import java.util.Locale;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class FisheryMemberAccountInterface extends MemberAccountAbstract implements FisheryInterface {

    public FisheryMemberAccountInterface(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        return onFisheryTrigger(event, args);
    }

    @Override
    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        return super.onTrigger(event, args);
    }

}
