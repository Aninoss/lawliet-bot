package commands.runnables.moderationcategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import core.utils.BotPermissionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;

@CommandProperties(
        trigger = "ban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83D\uDEAB",
        executableWithoutArgs = false
)
public class BanCommand extends WarnCommand {

    public BanCommand(Locale locale, String prefix) {
        super(locale, prefix, true, true, true);
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.ban(target.getId(), 1, reason)
                .submit()
                .exceptionally(e -> {
                    guild.ban(target.getId(), 1).queue();
                    return  null;
                });
    }

    @Override
    protected boolean canProcess(Member executor, User target) {
        return BotPermissionUtil.canInteract(executor.getGuild(), target);
    }

}
