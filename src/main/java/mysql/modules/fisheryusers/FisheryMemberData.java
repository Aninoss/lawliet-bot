package mysql.modules.fisheryusers;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import constants.CodeBlockColor;
import constants.FisheryGear;
import constants.LogStatus;
import constants.Settings;
import core.*;
import core.assets.MemberAsset;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.DBRedis;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

public class FisheryMemberData implements MemberAsset {

    private final long memberId;
    private final FisheryGuildData fisheryGuildBean;

    public final String KEY_ACCOUNT;

    public final String FIELD_FISH = "fish";
    public final String FIELD_COINS = "coins";
    public final String FIELD_COINS_GIVE_RECEIVED = "coins_give_received";
    public final String FIELD_COINS_GIVE_RECEIVED_MAX = "coins_give_received_max";
    public final String FIELD_DAILY_RECEIVED = "daily_received";
    public final String FIELD_DAILY_STREAK = "daily_streak";
    public final String FIELD_UPVOTE_STACK = "upvote_stack";
    public final String FIELD_REMINDER_SENT = "reminder_sent";
    public final String FIELD_BANNED_UNTIL = "banned_until";
    public final String FIELD_VOICE_MINUTES = "voice_minutes";
    public final String FIELD_DAILY_VALUES_UPDATED = "daily_values_updated";
    public final String FIELD_NEXT_WORK = "next_work";
    public final String FIELD_MESSAGES_THIS_HOUR = "messages_this_hour";
    public final String FIELD_MESSAGES_THIS_HOUR_SLOT = "messages_this_hour_slot";

    FisheryMemberData(FisheryGuildData fisheryGuildBean, long memberId) {
        this.fisheryGuildBean = fisheryGuildBean;
        this.memberId = memberId;
        this.KEY_ACCOUNT = "fishery_account:" + fisheryGuildBean.getGuildId() + ":" + memberId;
    }

    public FisheryGuildData getFisheryGuildData() {
        return fisheryGuildBean;
    }

    @Override
    public long getGuildId() {
        return fisheryGuildBean.getGuildId();
    }

    @Override
    public long getMemberId() {
        return memberId;
    }

    public List<Role> getRoles() {
        CustomObservableList<Role> allRoles = fisheryGuildBean.getRoles();

        ArrayList<Role> userRoles = new ArrayList<>();
        int level = getMemberGear(FisheryGear.ROLE).getLevel();

        if (level > allRoles.size()) {
            level = allRoles.size();
            setLevel(FisheryGear.ROLE, level);
        }

        if (level > 0) {
            if (fisheryGuildBean.getGuildData().isFisherySingleRoles()) {
                Role role = allRoles.get(level - 1);
                if (role != null) {
                    userRoles.add(role);
                }
            } else {
                for (int i = 0; i <= level - 1; i++) {
                    Role role = allRoles.get(i);
                    if (role != null) {
                        userRoles.add(role);
                    }
                }
            }
        }

        return userRoles;
    }

    public HashMap<Integer, FisheryMemberGearData> getGearMap() {
        HashMap<Integer, FisheryMemberGearData> map = new HashMap<>();
        for (int i = 0; i < FisheryGear.values().length; i++) {
            map.put(i, new FisheryMemberGearData(this, FisheryGear.values()[i]));
        }
        return map;
    }

    public FisheryMemberGearData getMemberGear(FisheryGear fisheryGear) {
        return new FisheryMemberGearData(this, fisheryGear);
    }

    public long getFish() {
        return DBRedis.getInstance().getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_FISH));
    }

    public long getCoins() {
        long coinsRaw = getCoinsRaw();
        long coinsHidden = getCoinsHidden();
        return coinsRaw - coinsHidden;
    }

    public long getCoinsRaw() {
        return DBRedis.getInstance().getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COINS));
    }

    public long getCoinsHidden() {
        return getFisheryGuildData().getCoinsHidden(memberId);
    }

    public long getCoinsGiveReceived() {
        cleanDailyValues();
        return DBRedis.getInstance().getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED));
    }

    public void addCoinsGiveReceived(long value) {
        cleanDailyValues();
        if (value > 0) {
            DBRedis.getInstance().update(jedis -> {
                long coinsGiveReceived = DBRedis.parseLong(jedis.hget(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED));
                long newCoinsGiveReceived = Math.min(coinsGiveReceived + value, Settings.FISHERY_MAX);
                jedis.hset(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED, String.valueOf(newCoinsGiveReceived));
            });
        }
    }

    public long getCoinsGiveReceivedMax() {
        cleanDailyValues();
        long coinsGiveReceivedMax = DBRedis.getInstance().getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED_MAX));
        if (coinsGiveReceivedMax == 0) {
            long sum = 0;
            for (FisheryGear gear : FisheryGear.values()) {
                sum += 15000L * FisheryMemberGearData.getValue(getMemberGear(gear).getLevel());
                if (sum >= Settings.FISHERY_MAX) {
                    return Settings.FISHERY_MAX;
                }
            }
            coinsGiveReceivedMax = sum;
            long finalCoinsGiveReceivedMax = coinsGiveReceivedMax;
            DBRedis.getInstance().update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED_MAX, String.valueOf(finalCoinsGiveReceivedMax)));
        }

        return coinsGiveReceivedMax;
    }

    public FisheryRecentFishGainsData getRecentFishGains() {
        return getFisheryGuildData().getRecentFishGainsForMember(memberId);
    }

    public LocalDate getDailyReceived() {
        LocalDate date = DBRedis.getInstance().getLocalDate(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DAILY_RECEIVED));
        return date != null ? date : LocalDate.of(2000, 1, 1);
    }

    public long getDailyStreak() {
        return DBRedis.getInstance().getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DAILY_STREAK));
    }

    public int getUpvoteStack() {
        return DBRedis.getInstance().getInteger(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_UPVOTE_STACK));
    }

    public boolean isReminderSent() {
        return DBRedis.getInstance().getBoolean(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_REMINDER_SENT));
    }

    public boolean isBanned() {
        Instant bannedUntil = DBRedis.getInstance().getInstant(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_BANNED_UNTIL));
        return bannedUntil != null && bannedUntil.isAfter(Instant.now());
    }

    public void cleanDailyValues() {
        LocalDate dailyValuesUpdated = DBRedis.getInstance().getLocalDate(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DAILY_VALUES_UPDATED));
        if (dailyValuesUpdated == null || LocalDate.now().isAfter(dailyValuesUpdated)) {
            DBRedis.getInstance().update(jedis -> {
                Pipeline pipeline = jedis.pipelined();
                pipeline.hset(KEY_ACCOUNT, FIELD_DAILY_VALUES_UPDATED, LocalDate.now().toString());
                pipeline.hdel(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED);
                pipeline.hdel(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED_MAX);
                pipeline.hdel(KEY_ACCOUNT, FIELD_VOICE_MINUTES);
                pipeline.sync();
            });
        }
    }

    public Optional<Instant> checkNextWork() {
        Instant nextWork = DBRedis.getInstance().getInstant(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_NEXT_WORK));
        boolean canWork = nextWork == null || Instant.now().isAfter(nextWork);
        if (canWork) {
            setWorkDone();
            return Optional.empty();
        } else {
            return Optional.of(nextWork);
        }
    }

    public void setWorkDone() {
        DBRedis.getInstance().update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_NEXT_WORK, Instant.now().plus(4, ChronoUnit.HOURS).toString()));
    }

    public void setWorkCanceled() {
        DBRedis.getInstance().update(jedis -> jedis.hdel(KEY_ACCOUNT, FIELD_NEXT_WORK));
    }

    public boolean registerMessage(Message message) {
        return DBRedis.getInstance().get(jedis -> {
            long hour = TimeUtil.currentHour();
            FisheryMemberGearData fisheryMemberGearData = getMemberGear(FisheryGear.MESSAGE);

            Pipeline pipeline = jedis.pipelined();
            Response<String> bannedUntilResp = pipeline.hget(KEY_ACCOUNT, FIELD_BANNED_UNTIL);
            Response<Long> messagesThisHourResp = pipeline.hincrBy(KEY_ACCOUNT, FIELD_MESSAGES_THIS_HOUR, 1);
            Response<String> messagesThisHourSlotResp = pipeline.hget(KEY_ACCOUNT, FIELD_MESSAGES_THIS_HOUR_SLOT);
            Response<String> fishResp = pipeline.hget(KEY_ACCOUNT, FIELD_FISH);
            Response<String> recentFishGainsRawResp = pipeline.hget(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId);
            Response<Double> recentFishGainsProcessedResp = pipeline.zscore(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, String.valueOf(memberId));
            Response<String> reminderSentResp = pipeline.hget(KEY_ACCOUNT, FIELD_REMINDER_SENT);
            Response<String> levelResp = pipeline.hget(KEY_ACCOUNT, fisheryMemberGearData.FIELD_GEAR);
            pipeline.sync();

            Instant bannedUntil = DBRedis.parseInstant(bannedUntilResp.get());
            if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
                return false;
            }

            long messagesThisHour = messagesThisHourResp.get();
            if (messagesThisHour >= 3400) {
                jedis.hset(KEY_ACCOUNT, FIELD_BANNED_UNTIL, Instant.now().plus(Duration.ofDays(3)).toString());
                MainLogger.get().warn("### User temporarily banned with id " + memberId);
                return false;
            }

            long messagesThisHourSlot = DBRedis.parseLong(messagesThisHourSlotResp.get());
            if (messagesThisHourSlot != hour) {
                pipeline = jedis.pipelined();
                pipeline.hdel(KEY_ACCOUNT, FIELD_MESSAGES_THIS_HOUR);
                pipeline.hset(KEY_ACCOUNT, FIELD_MESSAGES_THIS_HOUR_SLOT, String.valueOf(hour));
                pipeline.sync();
            }

            if (getFisheryGuildData().messageActivityIsValid(memberId, message.getContentRaw())) {
                long level = DBRedis.parseLong(levelResp.get());
                long effect = fisheryMemberGearData.getEffect(level);
                long fish = Math.min(DBRedis.parseLong(fishResp.get()) + effect, Settings.FISHERY_MAX);
                long recentFishGainsRaw = Math.min(DBRedis.parseLong(recentFishGainsRawResp.get()) + effect, Settings.FISHERY_MAX);
                long recentFishGainsProcessed = Math.min(DBRedis.parseLong(recentFishGainsProcessedResp.get()) + effect, Settings.FISHERY_MAX);

                pipeline.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(fish));
                pipeline.hset(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId, String.valueOf(recentFishGainsRaw));
                pipeline.zadd(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, recentFishGainsProcessed, String.valueOf(memberId));

                Optional<Member> memberOpt;
                if (fish >= 100 &&
                        !DBRedis.parseBoolean(reminderSentResp.get()) &&
                        getGuildData().isFisheryReminders() &&
                        BotPermissionUtil.canWriteEmbed(message.getTextChannel()) &&
                        (memberOpt = getMember()).isPresent()
                ) {
                    pipeline.hset(KEY_ACCOUNT, FIELD_REMINDER_SENT, "true");
                    Member member = memberOpt.get();
                    Locale locale = getGuildData().getLocale();
                    String prefix = getGuildData().getPrefix();

                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                            .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("{PREFIX}", prefix))
                            .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("{PREFIX}", prefix));
                    EmbedUtil.setMemberAuthor(eb, member);

                    message.getTextChannel().sendMessage(member.getAsMention())
                            .setEmbeds(eb.build())
                            .queue(m -> m.delete().queueAfter(Settings.FISHERY_DESPAWN_MINUTES, TimeUnit.MINUTES));
                }

                pipeline.sync();
                return true;
            } else {
                pipeline.sync();
            }
            return false;
        });
    }

    public void registerVoice(int minutes) throws ExecutionException {
        DBRedis.getInstance().update(jedis -> {
            long hour = TimeUtil.currentHour();
            int newMinutes = minutes;
            FisheryMemberGearData fisheryMemberGearData = getMemberGear(FisheryGear.VOICE);

            Pipeline pipeline = jedis.pipelined();
            Response<String> bannedUntilResp = pipeline.hget(KEY_ACCOUNT, FIELD_BANNED_UNTIL);
            Response<String> voiceMinutesResp = pipeline.hget(KEY_ACCOUNT, FIELD_VOICE_MINUTES);
            Response<String> fishResp = pipeline.hget(KEY_ACCOUNT, FIELD_FISH);
            Response<String> recentFishGainsRawResp = pipeline.hget(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId);
            Response<Double> recentFishGainsProcessedResp = pipeline.zscore(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, String.valueOf(memberId));
            Response<String> levelResp = pipeline.hget(KEY_ACCOUNT, fisheryMemberGearData.FIELD_GEAR);
            pipeline.sync();

            Instant bannedUntil = DBRedis.parseInstant(bannedUntilResp.get());
            if (bannedUntil == null || bannedUntil.isBefore(Instant.now())) {
                Optional<Integer> limitOpt = getGuildData().getFisheryVcHoursCap();
                if (limitOpt.isPresent() && ServerPatreonBoostCache.getInstance().get(getGuildId())) {
                    cleanDailyValues();
                    newMinutes = Math.min(newMinutes, limitOpt.get() * 60 - DBRedis.parseInteger(voiceMinutesResp.get()));
                }

                if (newMinutes > 0) {
                    long level = DBRedis.parseLong(levelResp.get());
                    long effect = fisheryMemberGearData.getEffect(level) * newMinutes;
                    long fish = Math.min(DBRedis.parseLong(fishResp.get()) + effect, Settings.FISHERY_MAX);
                    long recentFishGainsRaw = Math.min(DBRedis.parseLong(recentFishGainsRawResp.get()) + effect, Settings.FISHERY_MAX);
                    long recentFishGainsProcessed = Math.min(DBRedis.parseLong(recentFishGainsProcessedResp.get()) + effect, Settings.FISHERY_MAX);

                    pipeline = jedis.pipelined();
                    pipeline.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(fish));
                    pipeline.hset(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId, String.valueOf(recentFishGainsRaw));
                    pipeline.zadd(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, recentFishGainsProcessed, String.valueOf(memberId));
                    pipeline.hincrBy(KEY_ACCOUNT, FIELD_VOICE_MINUTES, newMinutes);
                    pipeline.sync();
                }
            }
        });
    }

    public void setFish(long fish) {
        long newFish = Math.max(Math.min(fish, Settings.FISHERY_MAX), 0);
        DBRedis.getInstance().update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(newFish)));
    }

    public void setCoinsRaw(long coins) {
        long coinsHidden = getCoinsHidden();
        long newCoins = Math.max(Math.min(coins, Settings.FISHERY_MAX), coinsHidden);
        DBRedis.getInstance().update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_COINS, String.valueOf(newCoins)));
    }

    public void addCoinsHidden(long value) {
        if (value != 0) {
            long coinsRaw = getCoinsRaw();
            getFisheryGuildData().addCoinsHidden(memberId, coinsRaw, value);
        }
    }

    public void addCoinsRaw(long value) {
        if (value != 0) {
            DBRedis.getInstance().update(jedis -> {
                long coinsRaw = DBRedis.parseLong(jedis.hget(KEY_ACCOUNT, FIELD_COINS));
                long coinsHidden = getCoinsHidden();
                long newCoins = Math.max(Math.min(coinsRaw + value, Settings.FISHERY_MAX), coinsHidden);
                jedis.hset(KEY_ACCOUNT, FIELD_COINS, String.valueOf(newCoins));
            });
        }
    }

    public void setDailyStreak(long dailyStreak) {
        long newDailyStreak = Math.max(Math.min(dailyStreak, Settings.FISHERY_MAX), 0);
        DBRedis.getInstance().update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_DAILY_STREAK, String.valueOf(newDailyStreak)));
    }

    public void changeValues(long fishAdd, long coinsAdd) {
        changeValues(fishAdd, coinsAdd, null);
    }

    public synchronized void changeValues(long fishAdd, long coinsAdd, Long newDailyStreak) {
        DBRedis.getInstance().update(jedis -> {
            long hour = TimeUtil.currentHour();

            Pipeline pipeline = jedis.pipelined();
            Response<String> fishResp = pipeline.hget(KEY_ACCOUNT, FIELD_FISH);
            Response<String> recentFishGainsRawResp = pipeline.hget(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId);
            Response<Double> recentFishGainsProcessedResp = pipeline.zscore(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, String.valueOf(memberId));
            Response<String> coinsResp = pipeline.hget(KEY_ACCOUNT, FIELD_COINS);
            pipeline.sync();

            pipeline = jedis.pipelined();

            if (fishAdd != 0) {
                long fish = Math.max(Math.min(DBRedis.parseLong(fishResp.get()) + fishAdd, Settings.FISHERY_MAX), 0);
                pipeline.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(fish));
                if (fishAdd > 0) {
                    long recentFishGainsRaw = Math.min(DBRedis.parseLong(recentFishGainsRawResp.get()) + fishAdd, Settings.FISHERY_MAX);
                    long recentFishGainsProcessed = Math.min(DBRedis.parseLong(recentFishGainsProcessedResp.get()) + fishAdd, Settings.FISHERY_MAX);
                    pipeline.hset(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId, String.valueOf(recentFishGainsRaw));
                    pipeline.zadd(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, recentFishGainsProcessed, String.valueOf(memberId));
                }
            }

            if (coinsAdd != 0) {
                long coinsHidden = getCoinsHidden();
                long coins = Math.max(Math.min(DBRedis.parseLong(coinsResp.get()) + coinsAdd, Settings.FISHERY_MAX), coinsHidden);
                pipeline.hset(KEY_ACCOUNT, FIELD_COINS, String.valueOf(coins));
            }

            if (newDailyStreak != null) {
                long newNewDailyStreak = Math.max(Math.min(newDailyStreak, Settings.FISHERY_MAX), 0);
                pipeline.hset(KEY_ACCOUNT, FIELD_DAILY_STREAK, String.valueOf(newNewDailyStreak));
            }

            pipeline.sync();
        });
    }

    public EmbedBuilder getAccountEmbed(Member member) {
        return changeValuesEmbed(member, 0, 0);
    }

    public EmbedBuilder changeValuesEmbed(Member member, long fishAdd, long coinsAdd) {
        return changeValuesEmbed(member, fishAdd, coinsAdd, null);
    }

    public synchronized EmbedBuilder changeValuesEmbed(Member member, long fishAdd, long coinsAdd, Long newDailyStreak) {
        return DBRedis.getInstance().get(jedis -> {
            long coinsHidden = getCoinsHidden();

            /* collect current data */
            Pipeline pipeline = jedis.pipelined();
            Response<String> fishPreviousResp = pipeline.hget(KEY_ACCOUNT, FIELD_FISH);
            Response<String> coinsPreviousResp = pipeline.hget(KEY_ACCOUNT, FIELD_COINS);
            Response<String> dailyStreakPreviousResp = pipeline.hget(KEY_ACCOUNT, FIELD_DAILY_STREAK);
            Response<String> bannedUntilResp = pipeline.hget(KEY_ACCOUNT, FIELD_BANNED_UNTIL);
            pipeline.sync();

            FisheryRecentFishGainsData fisheryRecentFishGainsDataPrevious = getRecentFishGains();
            long fishPrevious = DBRedis.parseLong(fishPreviousResp.get());
            long coinsPrevious = DBRedis.parseLong(coinsPreviousResp.get()) - coinsHidden;
            long dailyStreakPrevious = DBRedis.parseLong(dailyStreakPreviousResp.get());
            Instant bannedUntil = DBRedis.parseInstant(bannedUntilResp.get());
            boolean banned = bannedUntil != null && bannedUntil.isAfter(Instant.now());

            /* update values */
            FisheryRecentFishGainsData fisheryRecentFishGainsDataAfterwards = fisheryRecentFishGainsDataPrevious;
            if (fishAdd != 0 || coinsAdd != 0 || newDailyStreak != null) {
                changeValues(fishAdd, coinsAdd, newDailyStreak);
                fisheryRecentFishGainsDataAfterwards = getRecentFishGains();
            }

            /* generate account embed */
            Locale locale = getGuildData().getLocale();
            FisheryRecentFishGainsData finalFisheryRecentFishGainsDataAfterwards = fisheryRecentFishGainsDataAfterwards;
            return generateUserChangeEmbed(member, locale, fishAdd, coinsAdd,
                    finalFisheryRecentFishGainsDataAfterwards.getRank(), fisheryRecentFishGainsDataPrevious.getRank(),
                    finalFisheryRecentFishGainsDataAfterwards.getRecentFishGains(),
                    fisheryRecentFishGainsDataPrevious.getRecentFishGains(), fishPrevious, coinsPrevious, newDailyStreak,
                    dailyStreakPrevious, banned
            );
        });
    }

    private synchronized EmbedBuilder generateUserChangeEmbed(Member member, Locale locale, long fishAdd, long coinsAdd,
                                                              long rank, long rankPrevious, long fishIncome,
                                                              long fishIncomePrevious, long fishPrevious,
                                                              long coinsPrevious, Long newDailyStreak,
                                                              long dailyStreakPrevious, boolean isBanned
    ) {
        boolean patreon = PatreonCache.getInstance().getUserTier(memberId, false) >= 1;

        String patreonEmoji = "👑";
        String displayName = member.getEffectiveName();
        while (displayName.length() > 0 && displayName.startsWith(patreonEmoji)) {
            displayName = displayName.substring(patreonEmoji.length());
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor(TextManager.getString(locale, TextManager.GENERAL, "rankingprogress_title", patreon, displayName, patreonEmoji), null, member.getUser().getEffectiveAvatarUrl())
                .setThumbnail(member.getUser().getEffectiveAvatarUrl());

        if (patreon) eb.setColor(Color.YELLOW);
        if (fishAdd > 0 || (fishAdd == 0 && coinsAdd > 0)) {
            eb.setColor(Color.GREEN);
        } else if (coinsAdd <= 0 && (fishAdd < 0 || coinsAdd < 0)) {
            eb.setColor(Color.RED);
        }

        String codeBlock = CodeBlockColor.WHITE;
        if (rank < rankPrevious) {
            codeBlock = CodeBlockColor.GREEN;
        } else if (rank > rankPrevious) {
            codeBlock = CodeBlockColor.RED;
        }

        eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, "rankingprogress_desription",
                getEmbedSlot(locale, fishIncome, fishIncomePrevious, false),
                getEmbedSlot(locale, getFish(), fishPrevious, false),
                getEmbedSlot(locale, getCoins(), coinsPrevious, false),
                getEmbedSlot(locale, getDailyStreak(), newDailyStreak != null ? dailyStreakPrevious : getDailyStreak(), false),
                getEmbedSlot(locale, rank, rankPrevious, true),
                codeBlock
        ));

        if (isBanned) {
            EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "banned"));
        }

        return eb;
    }

    private String getEmbedSlot(Locale locale, long numberNow, long numberPrevious, boolean rankSlot) {
        long diff = numberNow - numberPrevious;
        String diffSign = diff >= 0 ? "+" : "";
        return TextManager.getString(locale, TextManager.GENERAL, rankSlot ? "rankingprogress_update2" : "rankingprogress_update", diff != 0,
                StringUtil.numToString(numberPrevious),
                StringUtil.numToString(numberNow),
                diffSign + StringUtil.numToString(diff)
        );
    }

    public void levelUp(FisheryGear gear) {
        getMemberGear(gear).levelUp();
    }

    public void setLevel(FisheryGear gear, int level) {
        getMemberGear(gear).setLevel(level);
    }

    public void updateDailyReceived() {
        if (!LocalDate.now().equals(getDailyReceived())) {
            DBRedis.getInstance().update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_DAILY_RECEIVED, LocalDate.now().toString()));
        }
    }

    public void addUpvote(int upvotes) {
        if (upvotes > 0) {
            DBRedis.getInstance().update(jedis -> jedis.hincrBy(KEY_ACCOUNT, FIELD_UPVOTE_STACK, upvotes));
        }
    }

    public void clearUpvoteStack() {
        DBRedis.getInstance().update(jedis -> jedis.hdel(KEY_ACCOUNT, FIELD_UPVOTE_STACK));
    }

    public void remove() {
        DBRedis.getInstance().update(jedis -> {
            List<Map.Entry<String, String>> recentFishGainsRaw = DBRedis.getInstance().hscan(jedis, getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW);

            Pipeline pipeline = jedis.pipelined();
            pipeline.del(KEY_ACCOUNT);
            pipeline.zrem(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, String.valueOf(memberId));
            String[] keysRemove = recentFishGainsRaw.stream()
                    .filter(entry -> {
                        long entryMemberId = Long.parseLong(entry.getKey().split(":")[1]);
                        return memberId == entryMemberId;
                    })
                    .map(Map.Entry::getKey)
                    .toArray(String[]::new);
            pipeline.hdel(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, keysRemove);
            pipeline.sync();
        });
    }

}