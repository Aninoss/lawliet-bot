package mysql.redis.fisheryusers;

import constants.*;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MainLogger;
import core.TextManager;
import core.assets.MemberAsset;
import core.cache.PatreonCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import modules.fishery.*;
import mysql.redis.RedisManager;
import mysql.hibernate.entity.FisheryEntity;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.autosell.DBAutoSell;
import mysql.modules.casinostats.DBCasinoStats;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.GuildVoiceState;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

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
    public final String FIELD_MESSAGES_ANICORD = "messages_anicord";
    public final String FIELD_DIAMONDS = "diamonds";
    public final String FIELD_POWERUP = "powerup";
    public final String FIELD_COUPONS = "coupons";

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

    public List<Role> getRoles(FisheryEntity fishery) {
        List<Role> allRoles = fishery.getRoles();

        ArrayList<Role> userRoles = new ArrayList<>();
        int level = getMemberGear(FisheryGear.ROLE).getLevel();

        if (level > allRoles.size()) {
            level = allRoles.size();
            setLevel(FisheryGear.ROLE, level);
        }

        if (level > 0) {
            if (fishery.getSingleRoles()) {
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

    public FisheryMemberStocksData getStocks(Stock stock) {
        return new FisheryMemberStocksData(this, stock);
    }

    public long getStocksTotalShares() {
        return RedisManager.get(jedis -> {
            ArrayList<Response<String>> sharesRespList = new ArrayList<>();
            Pipeline pipeline = jedis.pipelined();
            for (Stock stock : Stock.values()) {
                FisheryMemberStocksData stocksData = getStocks(stock);
                sharesRespList.add(pipeline.hget(KEY_ACCOUNT, stocksData.FIELD_SHARES));
            }
            pipeline.sync();

            long totalShares = 0;
            for (Response<String> stringResponse : sharesRespList) {
                totalShares += RedisManager.parseLong(stringResponse.get());
            }
            return totalShares;
        });
    }

    public long getStocksTotalInvestmentBefore() {
        return RedisManager.get(jedis -> {
            ArrayList<Response<String>> investmentRespList = new ArrayList<>();
            Pipeline pipeline = jedis.pipelined();
            for (Stock stock : Stock.values()) {
                FisheryMemberStocksData stocksData = getStocks(stock);
                investmentRespList.add(pipeline.hget(KEY_ACCOUNT, stocksData.FIELD_INVESTED));
            }
            pipeline.sync();

            long totalInvestment = 0;
            for (Response<String> stringResponse : investmentRespList) {
                totalInvestment += RedisManager.parseLong(stringResponse.get());
            }
            return totalInvestment;
        });
    }

    public long getStocksTotalInvestmentAfter() {
        return RedisManager.get(jedis -> {
            HashMap<Stock, Response<String>> sharesRespMap = new HashMap<>();
            Pipeline pipeline = jedis.pipelined();
            for (Stock stock : Stock.values()) {
                FisheryMemberStocksData stocksData = getStocks(stock);
                sharesRespMap.put(stock, pipeline.hget(KEY_ACCOUNT, stocksData.FIELD_SHARES));
            }
            pipeline.sync();

            long totalInvestment = 0;
            for (Map.Entry<Stock, Response<String>> stockResponseEntry : sharesRespMap.entrySet()) {
                totalInvestment += RedisManager.parseLong(stockResponseEntry.getValue().get()) * StockMarket.getValue(stockResponseEntry.getKey());
            }
            return totalInvestment;
        });
    }

    public int getCoupons() {
        return getActivePowerUps().contains(FisheryPowerUp.SHOP)
                ? Math.max(0, RedisManager.getInteger(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COUPONS)))
                : 0;
    }

    public void decreaseCoupons() {
        RedisManager.update(jedis -> {
            jedis.hincrBy(KEY_ACCOUNT, FIELD_COUPONS, -1);
        });
    }

    public void setCoupons(int coupons) {
        RedisManager.update(jedis -> {
            jedis.hset(KEY_ACCOUNT, FIELD_COUPONS, String.valueOf(coupons));
        });
    }

    public List<FisheryPowerUp> getActivePowerUps() {
        return RedisManager.get(jedis -> {
            HashMap<FisheryPowerUp, Response<String>> powerUpRespMap = new HashMap<>();
            ArrayList<FisheryPowerUp> powerUpList = new ArrayList<>();

            Pipeline pipeline = jedis.pipelined();
            for (FisheryPowerUp powerUp : FisheryPowerUp.values()) {
                powerUpRespMap.put(powerUp, pipeline.hget(KEY_ACCOUNT, FIELD_POWERUP + ":" + powerUp.ordinal()));
            }
            pipeline.sync();

            for (FisheryPowerUp powerUp : FisheryPowerUp.values()) {
                String response = powerUpRespMap.get(powerUp).get();
                if (response != null && Instant.now().isBefore(Instant.parse(response))) {
                    powerUpList.add(powerUp);
                }
            }

            return powerUpList;
        });
    }

    public void activatePowerUp(FisheryPowerUp powerUp, Instant expiration) {
        RedisManager.update(jedis -> {
            jedis.hset(KEY_ACCOUNT, FIELD_POWERUP + ":" + powerUp.ordinal(), expiration.toString());
        });
    }

    public void deletePowerUp(FisheryPowerUp powerUp) {
        RedisManager.update(jedis -> {
            jedis.hdel(KEY_ACCOUNT, FIELD_POWERUP + ":" + powerUp.ordinal());
        });
    }

    public long getFish() {
        return RedisManager.getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_FISH));
    }

    public long getCoins() {
        long coinsRaw = getCoinsRaw();
        long coinsHidden = getCoinsHidden();
        return coinsRaw - coinsHidden;
    }

    public long getCoinsRaw() {
        return RedisManager.getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COINS));
    }

    public long getCoinsHidden() {
        return getFisheryGuildData().getCoinsHidden(memberId);
    }

    public long getCoinsGiveReceived() {
        cleanDailyValues();
        return RedisManager.getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED));
    }

    public void addCoinsGiveReceived(long value) {
        cleanDailyValues();
        if (value > 0) {
            RedisManager.update(jedis -> {
                long coinsGiveReceived = RedisManager.parseLong(jedis.hget(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED));
                long newCoinsGiveReceived = Math.min(coinsGiveReceived + value, Settings.FISHERY_MAX);
                jedis.hset(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED, String.valueOf(newCoinsGiveReceived));
            });
        }
    }

    public long getCoinsGiveReceivedMax() {
        cleanDailyValues();
        long coinsGiveReceivedMax = RedisManager.getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED_MAX));
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
            RedisManager.update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_COINS_GIVE_RECEIVED_MAX, String.valueOf(finalCoinsGiveReceivedMax)));
        }

        return coinsGiveReceivedMax;
    }

    public void increaseDiamonds() {
        RedisManager.update(jedis -> {
            jedis.hincrBy(KEY_ACCOUNT, FIELD_DIAMONDS, 1);
        });
    }

    public int getDiamonds() {
        return RedisManager.getInteger(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DIAMONDS));
    }

    public void removeThreeDiamonds() {
        RedisManager.update(jedis -> {
            jedis.hincrBy(KEY_ACCOUNT, FIELD_DIAMONDS, -3);
        });
    }

    public FisheryRecentFishGainsData getRecentFishGains() {
        return getFisheryGuildData().getRecentFishGainsForMember(memberId);
    }

    public LocalDate getDailyReceived() {
        LocalDate date = RedisManager.getLocalDate(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DAILY_RECEIVED));
        return date != null ? date : LocalDate.of(2000, 1, 1);
    }

    public long getDailyStreak() {
        return RedisManager.getLong(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DAILY_STREAK));
    }

    public int getUpvoteStack() {
        return RedisManager.getInteger(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_UPVOTE_STACK));
    }

    public int getMessagesAnicord() {
        return RedisManager.getInteger(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_MESSAGES_ANICORD));
    }

    public boolean isReminderSent() {
        return RedisManager.getBoolean(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_REMINDER_SENT));
    }

    public boolean isBanned() {
        Instant bannedUntil = RedisManager.getInstant(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_BANNED_UNTIL));
        return bannedUntil != null && bannedUntil.isAfter(Instant.now());
    }

    public void cleanDailyValues() {
        LocalDate dailyValuesUpdated = RedisManager.getLocalDate(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_DAILY_VALUES_UPDATED));
        if (dailyValuesUpdated == null || LocalDate.now().isAfter(dailyValuesUpdated)) {
            RedisManager.update(jedis -> {
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
        Instant nextWork = RedisManager.getInstant(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_NEXT_WORK));
        boolean canWork = nextWork == null || Instant.now().isAfter(nextWork);
        if (canWork) {
            completeWork();
            return Optional.empty();
        } else {
            return Optional.of(nextWork);
        }
    }

    public Optional<Instant> getNextWork() {
        Instant nextWork = RedisManager.getInstant(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_NEXT_WORK));
        return Optional.ofNullable(nextWork);
    }

    public void completeWork() {
        RedisManager.update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_NEXT_WORK, Instant.now().plus(4, ChronoUnit.HOURS).toString()));
    }

    public void removeWork() {
        RedisManager.update(jedis -> jedis.hdel(KEY_ACCOUNT, FIELD_NEXT_WORK));
    }

    public boolean registerMessage(Message message, GuildEntity guildEntity) {
        return RedisManager.get(jedis -> {
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

            Instant bannedUntil = RedisManager.parseInstant(bannedUntilResp.get());
            if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
                return false;
            }

            long messagesThisHour = messagesThisHourResp.get();
            if (messagesThisHour >= 3400) {
                jedis.hset(KEY_ACCOUNT, FIELD_BANNED_UNTIL, Instant.now().plus(Duration.ofDays(3)).toString());
                MainLogger.get().warn("### User temporarily banned with id " + memberId);
                return false;
            }

            long messagesThisHourSlot = RedisManager.parseLong(messagesThisHourSlotResp.get());
            if (messagesThisHourSlot != hour) {
                pipeline = jedis.pipelined();
                pipeline.hdel(KEY_ACCOUNT, FIELD_MESSAGES_THIS_HOUR);
                pipeline.hset(KEY_ACCOUNT, FIELD_MESSAGES_THIS_HOUR_SLOT, String.valueOf(hour));
                pipeline.sync();
            }

            if (!getFisheryGuildData().messageActivityIsValid(memberId, message.getContentRaw())) {
                pipeline.sync();
                return false;
            }

            long level = RedisManager.parseLong(levelResp.get());
            long effect = fisheryMemberGearData.getEffect(level);
            if (getActivePowerUps().contains(FisheryPowerUp.LOUPE)) {
                effect += Math.round(effect * 0.25);
            }
            long fish = Math.min(RedisManager.parseLong(fishResp.get()) + effect, Settings.FISHERY_MAX);
            long recentFishGainsRaw = Math.min(RedisManager.parseLong(recentFishGainsRawResp.get()) + effect, Settings.FISHERY_MAX);
            long recentFishGainsProcessed = Math.min(RedisManager.parseLong(recentFishGainsProcessedResp.get()) + effect, Settings.FISHERY_MAX);

            pipeline.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(fish));
            pipeline.hset(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId, String.valueOf(recentFishGainsRaw));
            pipeline.zadd(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, recentFishGainsProcessed, String.valueOf(memberId));

            if (message.getGuild().getIdLong() == AnicordVerificationIds.GUILD_ID) {
                pipeline.hincrBy(KEY_ACCOUNT, FIELD_MESSAGES_ANICORD, 1);
            }

            Optional<Member> memberOpt;
            if (fish >= 100 &&
                    !RedisManager.parseBoolean(reminderSentResp.get()) &&
                    guildEntity.getFishery().getFishReminders() &&
                    BotPermissionUtil.canWriteEmbed(message.getGuildChannel()) &&
                    (memberOpt = getMember()).isPresent()
            ) {
                pipeline.hset(KEY_ACCOUNT, FIELD_REMINDER_SENT, "true");
                Member member = memberOpt.get();
                Locale locale = guildEntity.getLocale();
                String prefix = guildEntity.getPrefix();

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                        .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("{PREFIX}", prefix))
                        .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("{PREFIX}", prefix));
                EmbedUtil.setMemberAuthor(eb, member);

                message.getGuildChannel().sendMessage(member.getAsMention())
                        .setEmbeds(eb.build())
                        .queue(m -> m.delete()
                                .submitAfter(Settings.FISHERY_DESPAWN_MINUTES, TimeUnit.MINUTES)
                                .exceptionally(ExceptionLogger.get(ExceptionIds.UNKNOWN_MESSAGE, ExceptionIds.UNKNOWN_CHANNEL))
                        );
            }

            pipeline.sync();
            return true;
        });
    }

    public void registerVoice(GuildEntity guildEntity, int minutes) throws ExecutionException {
        RedisManager.update(jedis -> {
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

            Instant bannedUntil = RedisManager.parseInstant(bannedUntilResp.get());
            if (bannedUntil != null && bannedUntil.isAfter(Instant.now())) {
                return;
            }

            int limit = guildEntity.getFishery().getVoiceHoursLimitEffectively();
            if (limit != 5) {
                FeatureLogger.inc(PremiumFeature.FISHERY, getGuildId());
            }
            if (limit != 24) {
                cleanDailyValues();
                newMinutes = Math.min(newMinutes, limit * 60 - RedisManager.parseInteger(voiceMinutesResp.get()));
            }

            if (newMinutes > 0) {
                long level = RedisManager.parseLong(levelResp.get());
                long effect = fisheryMemberGearData.getEffect(level) * newMinutes;
                if (getActivePowerUps().contains(FisheryPowerUp.LOUPE)) {
                    effect += Math.round(effect * 0.25);
                }
                long fish = Math.min(RedisManager.parseLong(fishResp.get()) + effect, Settings.FISHERY_MAX);
                long recentFishGainsRaw = Math.min(RedisManager.parseLong(recentFishGainsRawResp.get()) + effect, Settings.FISHERY_MAX);
                long recentFishGainsProcessed = Math.min(RedisManager.parseLong(recentFishGainsProcessedResp.get()) + effect, Settings.FISHERY_MAX);

                pipeline = jedis.pipelined();
                pipeline.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(fish));
                pipeline.hset(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId, String.valueOf(recentFishGainsRaw));
                pipeline.zadd(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, recentFishGainsProcessed, String.valueOf(memberId));
                pipeline.hincrBy(KEY_ACCOUNT, FIELD_VOICE_MINUTES, newMinutes);
                pipeline.sync();
            }
        });
    }

    public void setFish(long fish) {
        long newFish = Math.max(Math.min(fish, Settings.FISHERY_MAX), 0);
        RedisManager.update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(newFish)));
    }

    public void setCoinsRaw(long coins) {
        long coinsHidden = getCoinsHidden();
        long newCoins = Math.max(Math.min(coins, Settings.FISHERY_MAX), coinsHidden);
        RedisManager.update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_COINS, String.valueOf(newCoins)));
    }

    public void addCoinsHidden(long value) {
        if (value != 0) {
            long coinsRaw = getCoinsRaw();
            getFisheryGuildData().addCoinsHidden(memberId, coinsRaw, value);
        }
    }

    public void addCoinsRaw(long value) {
        if (value != 0) {
            RedisManager.update(jedis -> {
                long coinsRaw = RedisManager.parseLong(jedis.hget(KEY_ACCOUNT, FIELD_COINS));
                long coinsHidden = getCoinsHidden();
                long newCoins = Math.max(Math.min(coinsRaw + value, Settings.FISHERY_MAX), coinsHidden);
                jedis.hset(KEY_ACCOUNT, FIELD_COINS, String.valueOf(newCoins));
            });
        }
    }

    public void setDailyStreak(long dailyStreak) {
        long newDailyStreak = Math.max(Math.min(dailyStreak, Settings.FISHERY_MAX), 0);
        RedisManager.update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_DAILY_STREAK, String.valueOf(newDailyStreak)));
    }

    public void changeValues(long fishAdd, long coinsAdd) {
        changeValues(fishAdd, coinsAdd, null);
    }

    public synchronized void changeValues(long fishAdd, long coinsAdd, Long newDailyStreak) {
        RedisManager.update(jedis -> {
            long hour = TimeUtil.currentHour();

            Pipeline pipeline = jedis.pipelined();
            Response<String> fishResp = pipeline.hget(KEY_ACCOUNT, FIELD_FISH);
            Response<String> recentFishGainsRawResp = pipeline.hget(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId);
            Response<Double> recentFishGainsProcessedResp = pipeline.zscore(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, String.valueOf(memberId));
            Response<String> coinsResp = pipeline.hget(KEY_ACCOUNT, FIELD_COINS);
            pipeline.sync();

            long fish = Math.max(Math.min(RedisManager.parseLong(fishResp.get()) + fishAdd, Settings.FISHERY_MAX), 0);
            pipeline = jedis.pipelined();

            if (fishAdd != 0) {
                pipeline.hset(KEY_ACCOUNT, FIELD_FISH, String.valueOf(fish));
                if (fishAdd > 0) {
                    long recentFishGainsRaw = Math.min(RedisManager.parseLong(recentFishGainsRawResp.get()) + fishAdd, Settings.FISHERY_MAX);
                    long recentFishGainsProcessed = Math.min(RedisManager.parseLong(recentFishGainsProcessedResp.get()) + fishAdd, Settings.FISHERY_MAX);
                    pipeline.hset(getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW, hour + ":" + memberId, String.valueOf(recentFishGainsRaw));
                    pipeline.zadd(getFisheryGuildData().KEY_RECENT_FISH_GAINS_PROCESSED, recentFishGainsProcessed, String.valueOf(memberId));
                }
            }

            if (coinsAdd != 0) {
                long coinsHidden = getCoinsHidden();
                long coins = Math.max(Math.min(RedisManager.parseLong(coinsResp.get()) + coinsAdd, Settings.FISHERY_MAX), coinsHidden);
                pipeline.hset(KEY_ACCOUNT, FIELD_COINS, String.valueOf(coins));
            }

            if (newDailyStreak != null) {
                long newNewDailyStreak = Math.max(Math.min(newDailyStreak, Settings.FISHERY_MAX), 0);
                pipeline.hset(KEY_ACCOUNT, FIELD_DAILY_STREAK, String.valueOf(newNewDailyStreak));
            }

            pipeline.sync();
        });
    }

    public EmbedBuilder getAccountEmbed(Member member, GuildEntity guildEntity) {
        return changeValuesEmbed(member, 0, 0, guildEntity);
    }

    public EmbedBuilder changeValuesEmbed(Member member, long fishAdd, long coinsAdd, GuildEntity guildEntity) {
        return changeValuesEmbed(member, fishAdd, coinsAdd, null, guildEntity);
    }

    public synchronized EmbedBuilder changeValuesEmbed(Member member, long fishAdd, long coinsAdd, Long newDailyStreak, GuildEntity guildEntity) {
        return RedisManager.get(jedis -> {
            long coinsHidden = getCoinsHidden();

            /* collect current data */
            Pipeline pipeline = jedis.pipelined();
            Response<String> fishPreviousResp = pipeline.hget(KEY_ACCOUNT, FIELD_FISH);
            Response<String> coinsPreviousResp = pipeline.hget(KEY_ACCOUNT, FIELD_COINS);
            Response<String> dailyStreakPreviousResp = pipeline.hget(KEY_ACCOUNT, FIELD_DAILY_STREAK);
            Response<String> bannedUntilResp = pipeline.hget(KEY_ACCOUNT, FIELD_BANNED_UNTIL);
            pipeline.sync();

            FisheryRecentFishGainsData fisheryRecentFishGainsDataPrevious = getRecentFishGains();
            long fishPrevious = RedisManager.parseLong(fishPreviousResp.get());
            long coinsPrevious = RedisManager.parseLong(coinsPreviousResp.get()) - coinsHidden;
            long dailyStreakPrevious = RedisManager.parseLong(dailyStreakPreviousResp.get());
            Instant bannedUntil = RedisManager.parseInstant(bannedUntilResp.get());
            boolean banned = bannedUntil != null && bannedUntil.isAfter(Instant.now());

            /* update values */
            FisheryRecentFishGainsData fisheryRecentFishGainsDataAfterwards = fisheryRecentFishGainsDataPrevious;
            if (fishAdd != 0 || coinsAdd != 0 || newDailyStreak != null) {
                changeValues(fishAdd, coinsAdd, newDailyStreak);
                fisheryRecentFishGainsDataAfterwards = getRecentFishGains();
            }

            /* generate account embed */
            FisheryRecentFishGainsData finalFisheryRecentFishGainsDataAfterwards = fisheryRecentFishGainsDataAfterwards;
            return generateUserChangeEmbed(member, guildEntity, fishAdd, coinsAdd,
                    finalFisheryRecentFishGainsDataAfterwards.getRank(), fisheryRecentFishGainsDataPrevious.getRank(),
                    finalFisheryRecentFishGainsDataAfterwards.getRecentFishGains(),
                    fisheryRecentFishGainsDataPrevious.getRecentFishGains(), fishPrevious, coinsPrevious, newDailyStreak,
                    dailyStreakPrevious, banned
            );
        });
    }

    private synchronized EmbedBuilder generateUserChangeEmbed(Member member, GuildEntity guildEntity, long fishAdd, long coinsAdd,
                                                              long rank, long rankPrevious, long fishIncome,
                                                              long fishIncomePrevious, long fishPrevious,
                                                              long coinsPrevious, Long newDailyStreak,
                                                              long dailyStreakPrevious, boolean isBanned
    ) {
        Locale locale = guildEntity.getLocale();
        boolean patreon = PatreonCache.getInstance().hasPremium(memberId, false);

        String patreonEmoji = "👑";
        String displayName = member.getEffectiveName();
        while (displayName.length() > 0 && displayName.startsWith(patreonEmoji)) {
            displayName = displayName.substring(patreonEmoji.length());
        }

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setAuthor(TextManager.getString(locale, TextManager.GENERAL, "rankingprogress_title", patreon, displayName, patreonEmoji), null, member.getEffectiveAvatarUrl())
                .setThumbnail(member.getEffectiveAvatarUrl());

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

        List<FisheryPowerUp> activePowerUps = getActivePowerUps();
        StringBuilder activePowerUpsStringBuilder = new StringBuilder();
        if (!activePowerUps.isEmpty()) {
            for (int i = 0; i < activePowerUps.size(); i++) {
                if (i > 0) {
                    activePowerUpsStringBuilder.append(" ");
                }
                activePowerUpsStringBuilder.append(activePowerUps.get(i).getEmoji().getFormatted());
            }
        } else {
            activePowerUpsStringBuilder.append(TextManager.getString(locale, TextManager.GENERAL, "rankingprogress_none"));
        }

        eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, guildEntity.getFishery().getPowerUps() ? "rankingprogress_desription_powerups" : "rankingprogress_desription",
                getEmbedSlot(locale, fishIncome, fishIncomePrevious, false),
                getEmbedSlot(locale, getFish(), fishPrevious, false),
                getEmbedSlot(locale, getCoins(), coinsPrevious, false),
                getEmbedSlot(locale, getDailyStreak(), newDailyStreak != null ? dailyStreakPrevious : getDailyStreak(), false),
                activePowerUpsStringBuilder.toString(),
                getEmbedSlot(locale, rank, rankPrevious, true),
                codeBlock
        ));

        if (isBanned) {
            EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "banned"));
        } else {
            Boolean voiceStatus = getVoiceStatus(guildEntity, member);
            if (voiceStatus != null) {
                EmbedUtil.addLog(
                        eb,
                        null,
                        TextManager.getString(locale, TextManager.GENERAL, "voice_activity", voiceStatus)
                );
            }
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

    public Boolean getVoiceStatus(GuildEntity guildEntity, Member member) {
        cleanDailyValues();
        GuildVoiceState guildVoiceState = member.getVoiceState();
        VoiceChannel voiceChannel;
        if (guildVoiceState != null &&
                guildVoiceState.inAudioChannel() &&
                guildVoiceState.getChannel() != null &&
                guildVoiceState.getChannel() instanceof VoiceChannel
        ) {
            voiceChannel = (VoiceChannel) guildVoiceState.getChannel();
            boolean active = Fishery.getValidVoiceMembers(guildEntity.getEntityManager(), voiceChannel).contains(member);
            if (active) {
                int voiceMinutes = RedisManager.getInteger(jedis -> jedis.hget(KEY_ACCOUNT, FIELD_VOICE_MINUTES));
                int voiceLimitMinutes = guildEntity.getFishery().getVoiceHoursLimitEffectively() * 60;
                return voiceMinutes < voiceLimitMinutes;
            } else {
                return false;
            }
        } else {
            return null;
        }
    }

    public void levelUp(FisheryGear gear) {
        getMemberGear(gear).levelUp();
    }

    public void setLevel(FisheryGear gear, int level) {
        getMemberGear(gear).setLevel(level);
    }

    public void updateDailyReceived() {
        if (!LocalDate.now().equals(getDailyReceived())) {
            RedisManager.update(jedis -> jedis.hset(KEY_ACCOUNT, FIELD_DAILY_RECEIVED, LocalDate.now().toString()));
        }
    }

    public void addUpvote(int upvotes) {
        if (upvotes > 0) {
            RedisManager.update(jedis -> jedis.hincrBy(KEY_ACCOUNT, FIELD_UPVOTE_STACK, upvotes));
        }
    }

    public void clearUpvoteStack() {
        RedisManager.update(jedis -> jedis.hdel(KEY_ACCOUNT, FIELD_UPVOTE_STACK));
    }

    public void remove() {
        DBCasinoStats.getInstance().removeMember(getGuildId(), getMemberId());
        RedisManager.update(jedis -> {
            List<Map.Entry<String, String>> recentFishGainsRaw = RedisManager.hscan(jedis, getFisheryGuildData().KEY_RECENT_FISH_GAINS_RAW);

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

    public boolean processAutoSell() {
        Integer autoSellThreshold = DBAutoSell.getInstance().retrieve().getThreshold(memberId);
        int exchangeRate = ExchangeRate.get(0);

        if (autoSellThreshold != null && exchangeRate >= autoSellThreshold) {
            long fish = getFish();
            if (fish > 0) {
                changeValues(-fish, fish * exchangeRate);
                return true;
            }
        }

        return false;
    }

}