package events.discordevents.guildjoin;

import constants.AssetIds;
import core.MainLogger;
import core.Program;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;

@DiscordEvent
public class GuildJoinNotifyBotOwner extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event, EntityManagerWrapper entityManager) {
        if (event.getGuild().getMemberCount() >= 100_000 && Program.publicInstance()) {
            JDAUtil.openPrivateChannel(event.getJDA(), AssetIds.OWNER_USER_ID)
                    .flatMap(messageChannel -> messageChannel.sendMessage("**+++** " + StringUtil.escapeMarkdown(event.getGuild().getName()) + " (" + event.getGuild().getMemberCount() + ")"))
                    .queue();
        }

        MainLogger.get().info("+++ {} ({}; {} members)", event.getGuild().getName(), event.getGuild().getIdLong(), event.getGuild().getMemberCount());
        return true;
    }

}
