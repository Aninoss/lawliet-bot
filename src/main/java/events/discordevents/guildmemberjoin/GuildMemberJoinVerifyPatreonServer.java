package events.discordevents.guildmemberjoin;

import constants.AssetIds;
import core.MainLogger;
import core.Program;
import core.cache.PatreonCache;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

@DiscordEvent
public class GuildMemberJoinVerifyPatreonServer extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event) throws Throwable {
        Member member = event.getMember();
        if (event.getGuild().getIdLong() == AssetIds.BETA_SERVER_ID &&
                Program.productionMode() &&
                !member.getUser().isBot() &&
                !PatreonCache.getInstance().hasPremium(member.getIdLong(), false)
        ) {
            MainLogger.get().info("Kicking {} due to joining the beta server without a premium subscription", member.getId());
            String text = "You need to be a Lawliet premium subscriber to join this server: https://lawlietbot.xyz/premium";
            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessage(text))
                    .submit()
                    .thenRun(() -> event.getGuild().kick(member).queue());
            return false;
        }

        return true;
    }

}
