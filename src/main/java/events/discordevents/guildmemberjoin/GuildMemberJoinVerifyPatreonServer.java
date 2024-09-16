package events.discordevents.guildmemberjoin;

import constants.AssetIds;
import core.MainLogger;
import core.Program;
import core.cache.PatreonCache;
import core.schedule.MainScheduler;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildMemberJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.DiscordSubscriptionEntity;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;

import java.time.Duration;

@DiscordEvent
public class GuildMemberJoinVerifyPatreonServer extends GuildMemberJoinAbstract {

    @Override
    public boolean onGuildMemberJoin(GuildMemberJoinEvent event, EntityManagerWrapper entityManager) throws Throwable {
        Member member = event.getMember();
        if (event.getGuild().getIdLong() == AssetIds.BETA_SERVER_ID &&
                Program.productionMode() &&
                !member.getUser().isBot() &&
                !PatreonCache.getInstance().hasPremium(member.getIdLong(), false) &&
                DiscordSubscriptionEntity.findValidDiscordSubscriptionEntitiesByUserId(entityManager, member.getIdLong()).isEmpty()
        ) {
            MainLogger.get().info("Kicking {} due to joining the beta server without a premium subscription", member.getId());
            String text = "You need to be a Lawliet premium subscriber to join this server: https://lawlietbot.xyz/premium";
            JDAUtil.openPrivateChannel(member)
                    .flatMap(messageChannel -> messageChannel.sendMessage(text))
                    .submit()
                    .thenRun(() -> event.getGuild().kick(member).queue());

            MainScheduler.schedule(Duration.ofSeconds(5), () -> {
                if (event.getGuild().getMembers().contains(member)) {
                    MainLogger.get().info("Member is still present, trying to kick again");
                    event.getGuild().kick(member).queue();
                }
            });
            return false;
        }

        return true;
    }

}
