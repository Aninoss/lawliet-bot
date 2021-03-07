package commands.runnables;

import java.util.Locale;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public abstract class FisheryUserAccountAbstract extends UserAccountAbstract implements FisheryAbstract {

    public FisheryUserAccountAbstract(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) throws Throwable {
        return ((FisheryAbstract) this).onTrigger(event, args);
    }

    public boolean onFisheryAccess(GuildMessageReceivedEvent event, String args) throws Throwable {
        return ((UserAccountAbstract) this).onTrigger(event, args);
    }

}
