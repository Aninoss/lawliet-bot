package events.scheduleevents.events;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import constants.AssetIds;
import constants.ExceptionRunnable;
import core.MainLogger;
import core.ShardManager;
import core.utils.StringUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.jetbrains.annotations.NotNull;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class AnicordEventObserver implements ExceptionRunnable {

    private final LoadingCache<Long, Integer> eventVoiceMinutesCache = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofHours(12))
            .build(new CacheLoader<>() {
                @Override
                public Integer load(@NotNull Long key) {
                    return 0;
                }
            });

    @Override
    public void run() throws Throwable {
        Guild guild = ShardManager.getLocalGuildById(AssetIds.ANICORD_SERVER_ID).orElse(null);
        if (guild != null) {
            VoiceChannel eventVoice = guild.getVoiceChannelById(624379779897884702L);
            if (eventVoice != null && eventVoiceIsValid(eventVoice)) {
                MainLogger.get().info("Event voice channel is active!");
                for (Member member : eventVoice.getMembers()) {
                    int newMinutes = eventVoiceMinutesCache.get(member.getIdLong()) + 1;
                    eventVoiceMinutesCache.put(member.getIdLong(), newMinutes);
                    if (newMinutes == 60) {
                        DBFishery.getInstance().retrieve(guild.getIdLong())
                                .getMemberData(member.getIdLong())
                                .increaseDiamonds();
                        guild.getTextChannelById(623630982641352724L)
                                .sendMessage("**" + StringUtil.escapeMarkdown(member.getEffectiveName()) + "** hat durch die Teilnahme an dem Event einen **💎 Diamanten** erhalten!")
                                .queue();
                    }
                }
            }
        }
    }

    private boolean eventVoiceIsValid(VoiceChannel eventVoice) {
        return eventVoice.getMembers().stream()
                .anyMatch(member ->
                        member.getRoles().stream().anyMatch(role -> role.getIdLong() == 626764841889300480L));
    }

}
