package commands.runnables.moderationcategory;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import core.mention.MentionList;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;

@CommandProperties(
        trigger = "ban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "\uD83D\uDEAB",
        executableWithoutArgs = false
)
//TODO: can now ban with id?
public class BanCommand extends KickCommand {

    public BanCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected MentionList<User> getUserList(Message message, String args) throws ExecutionException, InterruptedException {
        MentionList<Member> memberMentionList = MentionUtil.getMembers(message, args);
        ArrayList<User> userList = memberMentionList.getList().stream()
                .map(Member::getUser)
                .collect(Collectors.toCollection(ArrayList::new));

        MentionList<User> userMentionList = MentionUtil.getUsersFromString(memberMentionList.getFilteredArgs()).get();
        userList.addAll(userMentionList.getList());

        return new MentionList<>(userMentionList.getFilteredArgs(), userList);
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

}
