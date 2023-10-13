package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Locale;

@CommandProperties(
        trigger = "kick",
        botGuildPermissions = Permission.KICK_MEMBERS,
        userGuildPermissions = Permission.KICK_MEMBERS,
        emoji = "\uD83D\uDEAA",
        executableWithoutArgs = false,
        requiresFullMemberCache = true
)
public class KickCommand extends WarnCommand {

    public KickCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false, false, true, false);
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.kick(target, reason)
                .submit()
                .exceptionally(e -> {
                    guild.kick(target).queue();
                    return null;
                });
    }

}
