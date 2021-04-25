package mysql.modules.fisheryusers;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import constants.CodeBlockColor;
import constants.FisheryGear;
import constants.LogStatus;
import constants.Settings;
import core.CustomObservableList;
import core.EmbedFactory;
import core.MainLogger;
import core.TextManager;
import core.assets.MemberAsset;
import core.cache.PatreonCache;
import core.cache.ServerPatreonBoostCache;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import core.utils.TimeUtil;
import mysql.DataWithGuild;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;

public class FisheryMemberData extends DataWithGuild implements MemberAsset {

    private final long memberId;
    private FisheryGuildData fisheryGuildBean = null;
    private final HashMap<Instant, FisheryHourlyIncomeData> fisheryHourlyIncomeMap;
    private final HashMap<Integer, FisheryMemberGearData> gearMap;
    private long fish;
    private long coins;
    private long dailyStreak;
    private LocalDate dailyReceived;
    private int upvoteStack;
    private int lastMessagePeriod = -1;
    private int lastMessageHour = -1;
    private int vcMinutes;
    private boolean reminderSent;
    private boolean changed = false;
    private boolean banned = false;
    private Boolean onServer = null;
    private Long fishIncome = null;
    private Instant fishIncomeUpdateTime = null;
    private long messagesThisHour = 0;
    private long coinsHidden = 0;
    private long coinsGiven;
    private Long coinsGivenMax = null;
    private String lastContent = null;
    private LocalDate dailyValuesUpdated;
    private Instant nextWork;

    FisheryMemberData(long guildId, long memberId, long fish, long coins, LocalDate dailyReceived, long dailyStreak, boolean reminderSent, int upvoteStack, LocalDate dailyValuesUpdated, int vcMinutes, long coinsGiven, Instant nextWork, HashMap<Instant, FisheryHourlyIncomeData> fisheryHourlyIncomeMap, HashMap<Integer, FisheryMemberGearData> gearMap) {
        super(guildId);
        this.memberId = memberId;
        this.fish = fish;
        this.coins = coins;
        this.dailyReceived = dailyReceived;
        this.dailyStreak = dailyStreak;
        this.reminderSent = reminderSent;
        this.upvoteStack = upvoteStack;
        this.fisheryHourlyIncomeMap = fisheryHourlyIncomeMap;
        this.vcMinutes = vcMinutes;
        this.coinsGiven = coinsGiven;
        this.gearMap = gearMap;
        this.dailyValuesUpdated = dailyValuesUpdated;
        this.nextWork = nextWork;

        for (int i = 0; i < FisheryGear.values().length; i++) {
            this.gearMap.putIfAbsent(i, new FisheryMemberGearData(guildId, memberId, FisheryGear.values()[i], 0));
        }
    }

    public FisheryMemberData(long serverId, long memberId, FisheryGuildData fisheryGuildBean, long fish, long coins, LocalDate dailyReceived, int dailyStreak, boolean reminderSent, int upvoteStack, LocalDate dailyValuesUpdated, int vcMinutes, long coinsGiven, Instant nextWork, HashMap<Instant, FisheryHourlyIncomeData> fisheryHourlyIncomeMap, HashMap<Integer, FisheryMemberGearData> gearMap) {
        this(serverId, memberId, fish, coins, dailyReceived, dailyStreak, reminderSent, upvoteStack, dailyValuesUpdated, vcMinutes, coinsGiven, nextWork, fisheryHourlyIncomeMap, gearMap);
        setFisheryServerBean(fisheryGuildBean);
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
            if (fisheryGuildBean.getGuildBean().isFisherySingleRoles()) {
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

    public FisheryGuildData getFisheryServerBean() {
        return fisheryGuildBean;
    }

    public HashMap<Integer, FisheryMemberGearData> getGearMap() {
        return gearMap;
    }

    public FisheryMemberGearData getMemberGear(FisheryGear fisheryGear) {
        return gearMap.computeIfAbsent(fisheryGear.ordinal(), k -> new FisheryMemberGearData(getGuildId(), memberId, fisheryGear, 0));
    }

    public List<FisheryHourlyIncomeData> getAllFishHourlyIncomeChanged() {
        return fisheryHourlyIncomeMap.values().stream()
                .filter(FisheryHourlyIncomeData::checkChanged)
                .collect(Collectors.toList());
    }

    public long getFish() {
        return fish;
    }

    public long getCoins() {
        return coins - coinsHidden;
    }

    public long getCoinsRaw() {
        return coins;
    }

    public long getCoinsHidden() {
        return coinsHidden;
    }

    public long getCoinsGiven() {
        cleanDailyValues();
        return coinsGiven;
    }

    public void addCoinsGiven(long value) {
        cleanDailyValues();
        if (value > 0) {
            coinsGiven += value;
            checkValuesBound();
            setChanged();
        }
    }

    public long getTotalProgressIndex() {
        long sum = 0;
        for (FisheryGear gear : FisheryGear.values()) {
            sum += FisheryMemberGearData.getValue(getMemberGear(gear).getLevel());
        }
        return sum;
    }

    public long getCoinsGivenMax() {
        cleanDailyValues();
        if (coinsGivenMax == null) {
            long sum = 0;
            for (FisheryGear gear : FisheryGear.values()) {
                sum += 15000L * FisheryMemberGearData.getValue(getMemberGear(gear).getLevel());
                if (sum >= Settings.FISHERY_MAX) {
                    return Settings.FISHERY_MAX;
                }
            }
            coinsGivenMax = sum;
        }

        return coinsGivenMax;
    }

    public int getRank() {
        try {
            int count = 1;
            for (FisheryMemberData userBean : new ArrayList<>(fisheryGuildBean.getUsers().values())) {
                if (userBean.isOnServer() && userIsRankedHigherThanMe(userBean)) {
                    count++;
                }
            }
            return count;
        } catch (ConcurrentModificationException e) {
            MainLogger.get().error("Concurrent modification exception", e);
            return 0;
        }
    }

    private boolean userIsRankedHigherThanMe(FisheryMemberData user) {
        return (user.getFishIncome() > getFishIncome()) ||
                (user.getFishIncome() == getFishIncome() && user.getFish() > getFish()) ||
                (user.getFishIncome() == getFishIncome() && user.getFish() == getFish() && user.getCoins() > getCoins());
    }

    public long getFishIncome() {
        Instant currentHourInstance = TimeUtil.instantRoundDownToHour(Instant.now());
        for (int i = 0; i < 3 && (fishIncome == null || fishIncomeUpdateTime == null || fishIncomeUpdateTime.isBefore(currentHourInstance)); i++) {
            try {
                long n = 0;

                Instant effectiveInstant = currentHourInstance.minus(7, ChronoUnit.DAYS);
                for (FisheryHourlyIncomeData fisheryHourlyIncomeData : fisheryHourlyIncomeMap.values()) {
                    if (!fisheryHourlyIncomeData.getTime().isBefore(effectiveInstant)) {
                        n += fisheryHourlyIncomeData.getFishIncome();
                    }
                }

                fishIncome = n;
                fishIncomeUpdateTime = currentHourInstance;
                checkValuesBound();
                break;
            } catch (Throwable e) {
                if (i == 2) {
                    MainLogger.get().error("Exception", e);
                }
            }
        }

        return fishIncome == null ? 0L : fishIncome;
    }

    private FisheryHourlyIncomeData getCurrentFisheryHourlyIncome() {
        Instant currentTimeHour = TimeUtil.instantRoundDownToHour(Instant.now());
        return fisheryHourlyIncomeMap.computeIfAbsent(currentTimeHour, k -> new FisheryHourlyIncomeData(getGuildId(), memberId, currentTimeHour, 0));
    }

    public LocalDate getDailyReceived() {
        return dailyReceived;
    }

    public long getDailyStreak() {
        return dailyStreak;
    }

    public int getUpvoteStack() {
        return upvoteStack;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public boolean isBanned() {
        return banned;
    }

    public LocalDate getDailyValuesUpdated() {
        return dailyValuesUpdated;
    }

    public int getVcMinutes() {
        cleanDailyValues();
        return vcMinutes;
    }

    private void addVcMinutes(int value) {
        cleanDailyValues();
        if (value > 0) {
            vcMinutes += value;
            setChanged();
        }
    }

    public void cleanDailyValues() {
        if (!dailyValuesUpdated.isEqual(LocalDate.now())) {
            dailyValuesUpdated = LocalDate.now();
            vcMinutes = 0;
            coinsGiven = 0;
            coinsGivenMax = null;
            setChanged();
        }
    }

    public Instant getNextWork() {
        return nextWork;
    }

    public Optional<Instant> checkNextWork() {
        boolean canWork = Instant.now().isAfter(nextWork);
        if (canWork) {
            nextWork = Instant.now().plus(4, ChronoUnit.HOURS);
            return Optional.empty();
        } else {
            return Optional.of(nextWork);
        }
    }

    public void setWorkDone() {
        nextWork = Instant.now().plus(4, ChronoUnit.HOURS);
        setChanged();
    }

    public void setWorkCanceled() {
        nextWork = Instant.now();
    }

    void setFisheryServerBean(FisheryGuildData fisheryGuildBean) {
        if (this.fisheryGuildBean == null) {
            this.fisheryGuildBean = fisheryGuildBean;
        }
    }

    public boolean registerMessage(Message message) {
        if (banned) {
            return false;
        }
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        messagesThisHour++;
        if (messagesThisHour >= 3400) {
            banned = true;
            MainLogger.get().warn("### User temporarily banned with id " + memberId);
            return false;
        }

        if (lastMessageHour != hour) {
            lastMessageHour = hour;
            messagesThisHour = 0;
        }

        if (!message.getContentRaw().equalsIgnoreCase(lastContent)) {
            lastContent = message.getContentRaw();
            int currentMessagePeriod = (Calendar.getInstance().get(Calendar.SECOND) + Calendar.getInstance().get(Calendar.MINUTE) * 60) / 20;
            if (currentMessagePeriod != lastMessagePeriod) {
                lastMessagePeriod = currentMessagePeriod;
                long effect = getMemberGear(FisheryGear.MESSAGE).getEffect();

                fish += effect;
                if (effect > 0) {
                    if (fishIncome != null) fishIncome += effect;
                    getCurrentFisheryHourlyIncome().add(effect);
                }
                checkValuesBound();
                setChanged();

                Optional<Member> memberOpt = getMember();
                if (fish >= 100 &&
                        !reminderSent &&
                        getGuildBean().isFisheryReminders() &&
                        BotPermissionUtil.canWriteEmbed(message.getTextChannel()) &&
                        memberOpt.isPresent()
                ) {
                    reminderSent = true;
                    Member member = memberOpt.get();
                    Locale locale = getGuildBean().getLocale();
                    String prefix = getGuildBean().getPrefix();

                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                            .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("{PREFIX}", prefix))
                            .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("{PREFIX}", prefix));
                    EmbedUtil.setMemberAuthor(eb, member);

                    message.getTextChannel().sendMessage(member.getAsMention())
                            .embed(eb.build())
                            .queue(m -> m.delete().queueAfter(Settings.FISHERY_DESPAWN_MINUTES, TimeUnit.MINUTES));
                }

                return true;
            }
        }
        return false;
    }

    public void registerVC(int minutes) throws ExecutionException {
        if (!banned) {
            Optional<Integer> limitOpt = getGuildBean().getFisheryVcHoursCap();
            if (limitOpt.isPresent() && ServerPatreonBoostCache.getInstance().get(getGuildId())) {
                minutes = Math.min(minutes, limitOpt.get() * 60 - getVcMinutes());
            }

            if (minutes > 0) {
                long effect = getMemberGear(FisheryGear.VOICE).getEffect() * minutes;

                fish += effect;
                if (effect > 0) {
                    if (fishIncome != null) fishIncome += effect;
                    getCurrentFisheryHourlyIncome().add(effect);
                }
                addVcMinutes(minutes);
                checkValuesBound();
                setChanged();
            }
        }
    }

    public EmbedBuilder getAccountEmbed() {
        return changeValuesEmbed(0, 0);
    }

    public void setFish(long fish) {
        if (this.fish != fish) {
            this.fish = fish;
            checkValuesBound();
            setChanged();
        }
    }

    public void addFish(long fish) {
        if (fish != 0) {
            this.fish += fish;
            if (fish > 0) {
                if (fishIncome != null) fishIncome += fish;
                getCurrentFisheryHourlyIncome().add(fish);
            }
            reminderSent = true;
            checkValuesBound();
            setChanged();
        }
    }

    public void setCoinsRaw(long coins) {
        if (this.coins != coins) {
            this.coins = coins;
            checkValuesBound();
            setChanged();
        }
    }

    public void addHiddenCoins(long amount) {
        coinsHidden = Math.max(0, Math.min(coins, coinsHidden + amount));
    }

    public void addCoinsRaw(long coins) {
        if (coins != 0) {
            this.coins += coins;
            reminderSent = true;
            checkValuesBound();
            setChanged();
        }
    }

    public void setDailyStreak(long dailyStreak) {
        if (this.dailyStreak != dailyStreak) {
            this.dailyStreak = dailyStreak;
            checkValuesBound();
            setChanged();
        }
    }

    public void changeValues(long fishAdd, long coinsAdd) {
        changeValues(fishAdd, coinsAdd, null);
    }

    public synchronized void changeValues(long fishAdd, long coinsAdd, Long newDailyStreak) {
        addFish(fishAdd);
        addCoinsRaw(coinsAdd);
        if (newDailyStreak != null) {
            setDailyStreak(newDailyStreak);
        }
    }

    public EmbedBuilder changeValuesEmbed(long fishAdd, long coinsAdd) {
        return changeValuesEmbed(fishAdd, coinsAdd, null);
    }

    public synchronized EmbedBuilder changeValuesEmbed(long fishAdd, long coinsAdd, Long newDailyStreak) {
        /* Collect Current Data */
        long fishIncomePrevious = getFishIncome();
        long fishPrevious = getFish();
        long coinsPrevious = getCoins();
        long rankPrevious = getRank();
        long dailyStreakPrevious = getDailyStreak();

        /* Update Changes */
        addFish(fishAdd);
        addCoinsRaw(coinsAdd);
        if (newDailyStreak != null) {
            setDailyStreak(newDailyStreak);
        }

        long rank = getRank();

        /* Generate Account Embed */
        Locale locale = getGuildBean().getLocale();
        return getGuild().map(guild -> guild.getMemberById(memberId))
                .map(member -> generateUserChangeEmbed(member, locale, fishAdd, coinsAdd, rank, rankPrevious,
                        fishIncomePrevious, fishPrevious, coinsPrevious, newDailyStreak, dailyStreakPrevious
                        )
                ).orElse(null);
    }

    private synchronized EmbedBuilder generateUserChangeEmbed(Member member, Locale locale, long fishAdd, long coinsAdd,
                                                              long rank, long rankPrevious, long fishIncomePrevious, long fishPrevious,
                                                              long coinsPrevious, Long newDailyStreak, long dailyStreakPrevious
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

        if (banned) {
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

    private void checkValuesBound() {
        if (fish > Settings.FISHERY_MAX) {
            fish = Settings.FISHERY_MAX;
        } else if (fish < 0) fish = 0;

        if (coins > Settings.FISHERY_MAX) {
            coins = Settings.FISHERY_MAX;
        } else if (coins < coinsHidden) coins = coinsHidden;

        if (fishIncome != null) {
            if (fishIncome > Settings.FISHERY_MAX) {
                fishIncome = Settings.FISHERY_MAX;
            } else if (fishIncome < 0) fishIncome = 0L;
        }

        if (dailyStreak > Settings.FISHERY_MAX) {
            dailyStreak = Settings.FISHERY_MAX;
        } else if (dailyStreak < 0) dailyStreak = 0;

        if (coinsGiven > Settings.FISHERY_MAX) {
            coinsGiven = Settings.FISHERY_MAX;
        } else if (coinsGiven < 0) coinsGiven = 0;
    }

    public void levelUp(FisheryGear gear) {
        getMemberGear(gear).setLevel(getMemberGear(gear).getLevel() + 1);
        setChanged();
    }

    public void setLevel(FisheryGear gear, int level) {
        getMemberGear(gear).setLevel(level);
        setChanged();
    }

    public void updateDailyReceived() {
        if (!LocalDate.now().equals(dailyReceived)) {
            dailyReceived = LocalDate.now();
            checkValuesBound();
            setChanged();
        }
    }

    public void addUpvote(int upvotes) {
        if (upvotes > 0) {
            upvoteStack += upvotes;
            setChanged();
        }
    }

    public void clearUpvoteStack() {
        if (upvoteStack > 0) {
            upvoteStack = 0;
            setChanged();
        }
    }

    public boolean isOnServer() {
        if (onServer == null) {
            onServer = getMember().isPresent();
        }

        return onServer;
    }

    public void setOnServer(boolean onServer) {
        this.onServer = onServer;
    }

    public void remove() {
        getFisheryServerBean().getUsers().remove(memberId);
        DBFishery.getInstance().removeFisheryUserBean(this);
    }

    public boolean checkChanged() {
        boolean changedTemp = changed;
        changed = false;
        return changedTemp;
    }

    public void setChanged() {
        fisheryGuildBean.update();
        changed = true;
    }

}