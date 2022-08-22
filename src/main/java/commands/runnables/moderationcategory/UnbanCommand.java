package commands.runnables.moderationcategory;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.mention.MentionList;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

@CommandProperties(
        trigger = "unban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "🌼",
        executableWithoutArgs = false
)
public class UnbanCommand extends WarnCommand {

    public UnbanCommand(Locale locale, String prefix) {
        super(locale, prefix, false, false, true, true);
    }

    @Override
    protected MentionList<User> getUserList(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        return MentionUtil.getUsersFromString(args, false).get();
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.unban(target)
                .reason(reason)
                .submit()
                .exceptionally(e -> {
                    guild.unban(target).queue();
                    return null;
                });
    }

}
