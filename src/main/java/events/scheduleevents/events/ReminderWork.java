package events.scheduleevents.events;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import commands.Category;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryStatus;
import core.EmbedFactory;
import core.MainLogger;
import core.ShardManager;
import core.TextManager;
import core.schedule.ScheduleInterface;
import core.utils.MentionUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.autowork.AutoWorkData;
import mysql.modules.autowork.DBAutoWork;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryGuildData;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.subs.DBSubs;
import mysql.modules.subs.SubSlot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.MINUTES)
public class ReminderWork implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        HashMap<Long, HashSet<Guild>> reminderGuildMap = new HashMap<>();
        AtomicInteger actions = new AtomicInteger(0);
        Map<Long, SubSlot> subMap = DBSubs.getInstance().retrieve(DBSubs.Command.WORK);

        ShardManager.getLocalGuilds().stream()
                .filter(guild -> {
                    try {
                        return DBGuild.getInstance().retrieve(guild.getIdLong()).getFisheryStatus() == FisheryStatus.ACTIVE;
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not get server bean", e);
                    }
                    return false;
                })
                .forEach(guild -> {
                    try {
                        FisheryGuildData fisheryGuildData = DBFishery.getInstance().retrieve(guild.getIdLong());
                        process(fisheryGuildData, guild, subMap, actions, reminderGuildMap);
                    } catch (Throwable e) {
                        MainLogger.get().error("Could not manage auto work", e);
                    }
                });
        MainLogger.get().info("Auto Work - {} Actions", actions.get());

        for (Map.Entry<Long, HashSet<Guild>> slot : reminderGuildMap.entrySet()) {
            long userId = slot.getKey();
            HashSet<Guild> guilds = slot.getValue();
            SubSlot sub = subMap.get(userId);
            if (sub != null) {
                Locale locale = sub.getLocale();
                String guildsMention = MentionUtil.getMentionedStringOfGuilds(locale, new ArrayList<>(guilds)).getMentionText();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(TextManager.getString(locale, Category.FISHERY, "work_message_title"))
                        .setDescription(TextManager.getString(locale, Category.FISHERY, "work_message_desc", guildsMention));
                sub.sendEmbed(locale, eb);
            }
        }
    }

    private void process(FisheryGuildData fisheryGuildData, Guild guild, Map<Long, SubSlot> subMap, AtomicInteger autoWorkActions, HashMap<Long, HashSet<Guild>> reminderGuildMap) {
        AutoWorkData autoWorkData = DBAutoWork.getInstance().retrieve();
        for (Member member : guild.getMembers()) {
            FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(member.getIdLong());

            /* reminder */
            SubSlot sub = subMap.get(member.getIdLong());
            if (sub != null) {
                Optional<Instant> nextWork = fisheryMemberData.getNextWork();
                if (nextWork.isPresent() && Instant.now().isAfter(nextWork.get())) {
                    fisheryMemberData.removeWork();
                    reminderGuildMap.computeIfAbsent(member.getIdLong(), k -> new HashSet<>()).add(guild);
                }
            }

            /* auto work */
            if (autoWorkData.isActive(member.getIdLong()) && fisheryMemberData.checkNextWork().isEmpty()) {
                long coins = fisheryMemberData.getMemberGear(FisheryGear.WORK).getEffect();
                fisheryMemberData.changeValues(0, coins);
                fisheryMemberData.completeWork();
                autoWorkActions.incrementAndGet();
            }
        }
    }

}
