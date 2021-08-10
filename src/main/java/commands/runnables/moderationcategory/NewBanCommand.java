package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

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
        guild.ban(target.getId(), 1, reason)
                .submit()
                .exceptionally(e -> {
                    guild.kick(target.getId()).queue();
                    return null;
                });
    }

}
