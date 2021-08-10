package commands.runnables.moderationcategory;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import core.mention.MentionList;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

@CommandProperties(
        trigger = "unban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83C\uDF3C",
        executableWithoutArgs = false
)
public class UnbanCommand extends WarnCommand {

    public UnbanCommand(Locale locale, String prefix) {
        super(locale, prefix, false, false, true, true);
    }

    @Override
    protected MentionList<User> getUserList(Message message, String args) {
        List<User> userBanList = message.getGuild().retrieveBanList().complete().stream()
                .map(Guild.Ban::getUser)
                .collect(Collectors.toList());

        return MentionUtil.getUsers(message, args, userBanList);
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.unban(target.getId())
                .reason(reason)
                .submit()
                .exceptionally(e -> {
                    guild.unban(target.getId()).queue();
                    return null;
                });
    }

}
