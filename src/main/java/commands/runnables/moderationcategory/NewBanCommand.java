package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

@CommandProperties(
        trigger = "newban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83D\uDEAB",
        executableWithoutArgs = false,
        patreonRequired = true,
        requiresFullMemberCache = true,
        aliases = { "newcomersban", "newcomerban", "bannew", "bannewcomer", "bannewcomers", "annihilate", "obliterate" }
)
public class NewBanCommand extends NewKickCommand {

    public NewBanCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.ban(target, 1, TimeUnit.DAYS)
                .reason(reason)
                .submit()
                .exceptionally(e -> {
                    guild.kick(target).queue();
                    return null;
                });
    }

    @Override
    protected BotLogEntity.Event getBotLogEvent() {
        return BotLogEntity.Event.MOD_BAN;
    }

}
