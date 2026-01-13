package events.scheduleevents.events;

import commands.Category;
import commands.Command;
import commands.runnables.birthdaycategory.BirthdayConfigCommand;
import constants.ExceptionRunnable;
import core.*;
import core.cache.UserWithWorkFisheryDmReminderCache;
import core.utils.MentionUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import modules.fishery.Fishery;
import modules.fishery.FisheryGear;
import modules.fishery.FisheryStatus;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.BirthdayEntity;
import mysql.hibernate.entity.guild.BirthdayUserEntryEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.hibernate.entity.user.FisheryDmReminderEntity;
import mysql.modules.autosell.AutoSellData;
import mysql.modules.autosell.DBAutoSell;
import mysql.modules.autowork.AutoWorkData;
import mysql.modules.autowork.DBAutoWork;
import mysql.redis.fisheryusers.FisheryGuildData;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

@ScheduleEventFixedRate(rateValue = GuildEntityProcessors.RATE_MINUTES, rateUnit = ChronoUnit.MINUTES)
public class GuildEntityProcessors implements ExceptionRunnable {

    public static final int RATE_MINUTES = 2;

    private static int lastBirthdayQuarter = -1;

    @Override
    public void run() throws Throwable {
        MainLogger.get().info("Starting processors");

        AtomicInteger voiceActivityActions = new AtomicInteger(0);
        AtomicInteger autoSellActions = new AtomicInteger(0);
        AtomicInteger autoWorkActions = new AtomicInteger(0);
        AtomicInteger birthdayActions = new AtomicInteger(0);

        int birthdayQuarter = LocalDateTime.now().getMinute() / 15;
        boolean checkBirthdays = birthdayQuarter != lastBirthdayQuarter;
        lastBirthdayQuarter = birthdayQuarter;
        HashMap<Long, HashSet<Guild>> reminderGuildMap = new HashMap<>();

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(GuildEntityProcessors.class, 2)) {
            for (Guild guild : ShardManager.getLocalGuilds()) {
                try {
                    GuildEntity guildEntity = entityManager.findGuildEntity(guild.getIdLong());
                    if (guildEntity.getFishery().getFisheryStatus() == FisheryStatus.ACTIVE) {
                        processVoiceActivity(guild, guildEntity, voiceActivityActions);
                        processAutoSell(guild, autoSellActions);
                        processAutoWork(guild, guildEntity, autoWorkActions, reminderGuildMap);
                    }
                    if (checkBirthdays && guildEntity.getBirthday().getActive()) {
                        processBirthday(guild, guildEntity, birthdayActions);
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Guild entity processor exception for {}", guild.getIdLong(), e);
                }
                entityManager.clear();
            }

            MainLogger.get().info("Voice Channel - {} Actions", voiceActivityActions.get());
            MainLogger.get().info("Auto Sell - {} Actions", autoSellActions.get());
            MainLogger.get().info("Auto Work - {} Actions", autoWorkActions.get());
            if (checkBirthdays) {
                MainLogger.get().info("Birthday - {} Actions", birthdayActions.get());
            }

            autoWorkPostProcessing(entityManager, reminderGuildMap);
        }
    }

    private void processVoiceActivity(Guild guild, GuildEntity guildEntity, AtomicInteger actions) {
        try {
            FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guild.getIdLong());
            for (VoiceChannel voiceChannel : guild.getVoiceChannels()) {
                try {
                    List<Member> validMembers = Fishery.getValidVoiceMembers(voiceChannel);
                    VoiceChannel afkVoice = guild.getAfkChannel();
                    if (afkVoice == null || voiceChannel.getIdLong() != afkVoice.getIdLong()) {
                        validMembers.forEach(member -> {
                            try {
                                fisheryGuildData.getMemberData(member.getIdLong()).registerVoice(guildEntity, RATE_MINUTES);
                                actions.incrementAndGet();
                            } catch (ExecutionException e) {
                                MainLogger.get().error("Exception when registering vc", e);
                            }
                        });
                    }
                } catch (Throwable e) {
                    MainLogger.get().error("Error while fetching voice member list", e);
                }
            }
        } catch (Throwable e) {
            MainLogger.get().error("Could not process voice channel activities for guild {}", guild.getIdLong(), e);
        }
    }

    private void processAutoSell(Guild guild, AtomicInteger actions) {
        try {
            FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guild.getIdLong());
            AutoSellData autoSellData = DBAutoSell.getInstance().retrieve();

            guild.getMembers().stream()
                    .filter(member -> autoSellData.getThreshold(member.getIdLong()) != null)
                    .forEach(member -> {
                        if (fisheryGuildData.getMemberData(member.getIdLong()).processAutoSell()) {
                            actions.incrementAndGet();
                        }
                    });
        } catch (Throwable e) {
            MainLogger.get().error("Could not process auto sell for guild {}", guild.getIdLong(), e);
        }
    }

    private void processAutoWork(Guild guild, GuildEntity guildEntity, AtomicInteger autoWorkActions, HashMap<Long, HashSet<Guild>> reminderGuildMap) {
        try {
            FisheryGuildData fisheryGuildData = FisheryUserManager.getGuildData(guild.getIdLong());
            AutoWorkData autoWorkData = DBAutoWork.getInstance().retrieve();
            for (Member member : guild.getMembers()) {
                FisheryMemberData fisheryMemberData = fisheryGuildData.getMemberData(member.getIdLong());

                /* reminder */
                if (UserWithWorkFisheryDmReminderCache.getInstance().hasWorkFisheryDmReminder(member.getIdLong())) {
                    Optional<Instant> nextWork = fisheryMemberData.getNextWork();
                    if (nextWork.isPresent() && Instant.now().isAfter(nextWork.get())) {
                        fisheryMemberData.removeWork();
                        reminderGuildMap.computeIfAbsent(member.getIdLong(), k -> new HashSet<>()).add(guild);
                    }
                }

                /* auto work */
                long workIntervalMinutes = guildEntity.getFishery().getWorkIntervalMinutesEffectively();
                if (autoWorkData.isActive(member.getIdLong()) && fisheryMemberData.checkNextWork(workIntervalMinutes).isEmpty()) {
                    long coins = fisheryMemberData.getMemberGear(FisheryGear.WORK).getEffect();
                    fisheryMemberData.changeValues(0, coins);
                    autoWorkActions.incrementAndGet();
                }
            }
        } catch (Throwable e) {
            MainLogger.get().error("Could not process auto work for guild {}", guild.getIdLong(), e);
        }
    }

    private synchronized void processBirthday(Guild guild, GuildEntity guildEntity, AtomicInteger birthdayActions) {
        BirthdayEntity birthday = guildEntity.getBirthday();
        for (Map.Entry<Long, BirthdayUserEntryEntity> setEntry : birthday.getUserEntries().entrySet()) {
            long userId = setEntry.getKey();
            BirthdayUserEntryEntity entry = setEntry.getValue();

            try {
                if (entry.isBirthday() && entry.getTriggered() == null) {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).get();
                    if (member == null) {
                        continue;
                    }

                    GuildMessageChannel channel = birthday.getChannel().get().orElse(null);
                    if (channel != null && PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), BirthdayConfigCommand.class, channel, Permission.MESSAGE_SEND)) {
                        channel.sendMessage(TextManager.getString(guildEntity.getLocale(), Category.BIRTHDAYS, "birthdayconfig_message", member.getAsMention()))
                                .queue();
                    }

                    Role role = birthday.getRole().get().orElse(null);
                    if (role != null && PermissionCheckRuntime.botCanManageRoles(guildEntity.getLocale(), BirthdayConfigCommand.class, role)) {
                        guild.addRoleToMember(member, role)
                                .reason(Command.getCommandLanguage(BirthdayConfigCommand.class, guildEntity.getLocale()).getTitle())
                                .queue();
                    }

                    guildEntity.beginTransaction();
                    entry.updateTriggerYear();
                    entry.setTriggered(true);
                    guildEntity.commitTransaction();
                    birthdayActions.incrementAndGet();
                } else if (!entry.isBirthday() && entry.getTriggered() != null) {
                    Member member = MemberCacheController.getInstance().loadMember(guild, userId).get();
                    if (member == null) {
                        continue;
                    }

                    Role role = birthday.getRole().get().orElse(null);
                    if (role != null && PermissionCheckRuntime.botCanManageRoles(guildEntity.getLocale(), BirthdayConfigCommand.class, role)) {
                        guild.removeRoleFromMember(member, role)
                                .reason(Command.getCommandLanguage(BirthdayConfigCommand.class, guildEntity.getLocale()).getTitle())
                                .queue();
                    }

                    guildEntity.beginTransaction();
                    if (entry.getTriggerYear() != null) {
                        entry.setTriggerYear(entry.getTriggerYear() + 1);
                    }
                    entry.setTriggered(null);
                    guildEntity.commitTransaction();
                    birthdayActions.incrementAndGet();
                }
            } catch (ExecutionException | InterruptedException e) {
                MainLogger.get().error("Exception when processing birthday for userId {}", userId, e);
            }
        }
    }

    private static void autoWorkPostProcessing(EntityManagerWrapper entityManager, HashMap<Long, HashSet<Guild>> reminderGuildMap) {
        try {
            for (Map.Entry<Long, HashSet<Guild>> slot : reminderGuildMap.entrySet()) {
                long userId = slot.getKey();
                HashSet<Guild> guilds = slot.getValue();

                if (!UserWithWorkFisheryDmReminderCache.getInstance().hasWorkFisheryDmReminder(userId)) {
                    continue;
                }

                FisheryDmReminderEntity fisheryDmReminderEntity = entityManager.findUserEntity(userId).getFisheryDmReminders().get(FisheryDmReminderEntity.Type.WORK);
                if (fisheryDmReminderEntity == null) {
                    continue;
                }

                Locale locale = fisheryDmReminderEntity.getLocale();
                String guildsMention = MentionUtil.getMentionedStringOfGuilds(locale, new ArrayList<>(guilds)).getMentionText();
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(TextManager.getString(locale, Category.FISHERY, "work_message_title"))
                        .setDescription(TextManager.getString(locale, Category.FISHERY, "work_message_desc", guildsMention));
                fisheryDmReminderEntity.sendEmbed(eb);

                entityManager.clear();
            }
        } catch (Throwable e) {
            MainLogger.get().error("Auto work post processing errors", e);
        }
    }

}
