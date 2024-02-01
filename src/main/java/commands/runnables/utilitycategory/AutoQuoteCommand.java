package commands.runnables.utilitycategory;

import commands.listeners.CommandProperties;
import commands.runnables.CommandOnOffSwitchAbstract;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.autoquote.DBAutoQuote;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Locale;

@CommandProperties(
        trigger = "autoquote",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDCDD",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class AutoQuoteCommand extends CommandOnOffSwitchAbstract {

    public AutoQuoteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

    @Override
    protected boolean isActive(Member member) {
        return DBAutoQuote.getInstance().retrieve(member.getGuild().getIdLong()).isActive();
    }

    @Override
    protected boolean setActive(Member member, boolean active) {
        DBAutoQuote.getInstance().retrieve(member.getGuild().getIdLong()).setActive(active);
        BotLogEntity.log(getEntityManager(), BotLogEntity.Event.AUTO_QUOTE, member, null, active);
        return true;
    }

}
