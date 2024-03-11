package commands.runnables.moderationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import core.MemberCacheController;
import core.mention.MentionList;
import core.utils.MentionUtil;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "unban",
        botGuildPermissions = Permission.BAN_MEMBERS,
        userGuildPermissions = Permission.BAN_MEMBERS,
        emoji = "🌼",
        executableWithoutArgs = false,
        requiresFullMemberCache = true
)
public class UnbanCommand extends WarnCommand {

    public UnbanCommand(Locale locale, String prefix) {
        super(locale, prefix, false, false, true, true, false, false);
    }

    @Override
    protected MentionList<User> getUserList(CommandEvent event, String args) throws ExecutionException, InterruptedException {
        return MentionUtil.getUsersFromString(args, false).get();
    }

    @Override
    protected void process(Guild guild, User target, String reason) throws ExecutionException, InterruptedException {
        if (MemberCacheController.getInstance().loadMember(guild, target.getIdLong()).get() != null) {
            return;
        }

        guild.unban(target)
                .reason(reason)
                .submit()
                .exceptionally(e -> {
                    guild.unban(target).queue();
                    return null;
                });
    }

    @Override
    protected BotLogEntity.Event getBotLogEvent() {
        return BotLogEntity.Event.MOD_UNBAN;
    }

}
