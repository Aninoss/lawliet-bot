package events.discordevents.guildjoin;

import core.MainLogger;
import core.Program;
import core.ShardManager;
import events.discordevents.DiscordEvent;
import events.discordevents.EventPriority;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent(priority = EventPriority.HIGH)
public class GuildJoinCheckNonPublicLimit extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event) {
        if (Program.publicVersion() ||
                ShardManager.getLocalGuilds().size() - 2 <= Integer.parseInt(System.getenv("MAX_SERVERS"))
        ) {
            return true;
        }

        MainLogger.get().warn("Leaving server {} due to server limits", event.getGuild().getIdLong());
        event.getGuild().leave().queue();
        return false;
    }

}
