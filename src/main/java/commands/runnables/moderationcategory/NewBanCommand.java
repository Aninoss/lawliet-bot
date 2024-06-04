package commands.runnables.moderationcategory;

import com.google.common.collect.Lists;
import commands.listeners.CommandProperties;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    protected void processAll(Guild guild, List<User> targets, String reason) {
        List<List<User>> partitionedTargets = Lists.partition(targets, 200);
        for (List<User> users : partitionedTargets) {
            List<UserSnowflake> userSnowflakes = users.stream().map(user -> UserSnowflake.fromId(user.getId())).collect(Collectors.toList());
            guild.ban(userSnowflakes, 1, TimeUnit.DAYS)
                    .reason(reason)
                    .submit()
                    .exceptionally(e -> {
                        guild.ban(userSnowflakes, 1, TimeUnit.DAYS).queue();
                        return null;
                    });
        }
    }

    @Override
    protected BotLogEntity.Event getBotLogEvent() {
        return BotLogEntity.Event.MOD_BAN;
    }

}
