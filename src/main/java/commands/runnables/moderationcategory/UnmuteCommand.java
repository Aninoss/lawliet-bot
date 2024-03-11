package commands.runnables.moderationcategory;

import commands.listeners.CommandProperties;
import mysql.hibernate.entity.BotLogEntity;
import net.dv8tion.jda.api.Permission;

import java.util.Locale;

@CommandProperties(
        trigger = "unmute",
        botGuildPermissions = Permission.MODERATE_MEMBERS,
        userGuildPermissions = Permission.MODERATE_MEMBERS,
        emoji = "🛑",
        executableWithoutArgs = false,
        releaseDate = { 2021, 4, 16 },
        requiresFullMemberCache = true,
        aliases = { "chunmute", "channelunmute", "demute" }
)
public class UnmuteCommand extends MuteCommand {

    public UnmuteCommand(Locale locale, String prefix) {
        super(locale, prefix, false);
    }

    @Override
    protected BotLogEntity.Event getBotLogEvent() {
        return BotLogEntity.Event.MOD_UNMUTE;
    }

}
