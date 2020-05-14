package MySQL.Modules.FisheryUsers;

import Constants.CodeBlockColor;
import Constants.FisheryCategoryInterface;
import Constants.LogStatus;
import Constants.Settings;
import Core.EmbedFactory;
import Core.PatreonCache;
import Core.ServerPatreonBoostCache;
import Core.TextManager;
import Core.Utils.BotUtil;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
import MySQL.BeanWithServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.*;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class FisheryUserBean extends BeanWithServer {

    final static Logger LOGGER = LoggerFactory.getLogger(FisheryUserBean.class);

    private final long userId;
    private FisheryServerBean fisheryServerBean = null;
    private final HashMap<Instant, FisheryHourlyIncomeBean> fisheryHourlyIncomeMap;
    private final HashMap<Integer, FisheryUserPowerUpBean> powerUpMap;
    private long fish, coins;
    private LocalDate dailyReceived;
    private int dailyStreak, upvoteStack, lastMessagePeriod = -1, lastMessageHour = -1, vcMinutes = 0;
    private boolean reminderSent, changed = false, banned = false;
    private Long fishIncome = null;
    private Instant fishIncomeUpdateTime = null;
    private long hiddenCoins = 0, messagesThisHour = 0;
    private String lastContent = null;

    FisheryUserBean(ServerBean serverBean, long userId, long fish, long coins, LocalDate dailyReceived, int dailyStreak, boolean reminderSent, int upvoteStack, HashMap<Instant, FisheryHourlyIncomeBean> fisheryHourlyIncomeMap, HashMap<Integer, FisheryUserPowerUpBean> powerUpMap) {
        super(serverBean);
        this.userId = userId;
        this.fish = fish;
        this.coins = coins;
        this.dailyReceived = dailyReceived;
        this.dailyStreak = dailyStreak;
        this.reminderSent = reminderSent;
        this.upvoteStack = upvoteStack;
        this.fisheryHourlyIncomeMap = fisheryHourlyIncomeMap;
        this.powerUpMap = powerUpMap;

        for(int i = 0; i < 6; i++) this.powerUpMap.putIfAbsent(i, new FisheryUserPowerUpBean(serverBean.getServerId(), userId, i, 0));
    }

    public FisheryUserBean(ServerBean serverBean, long userId, FisheryServerBean fisheryServerBean, long fish, long coins, LocalDate dailyReceived, int dailyStreak, boolean reminderSent, int upvoteStack, HashMap<Instant, FisheryHourlyIncomeBean> fisheryHourlyIncomeMap, HashMap<Integer, FisheryUserPowerUpBean> powerUpMap) {
        this(serverBean, userId, fish, coins, dailyReceived, dailyStreak, reminderSent, upvoteStack, fisheryHourlyIncomeMap, powerUpMap);
        setFisheryServerBean(fisheryServerBean);
    }


    /* Getters */

    public long getUserId() { return userId; }

    public Optional<User> getUser() { return getServer().flatMap(server -> server.getMemberById(userId)); }

    public FisheryServerBean getFisheryServerBean() { return fisheryServerBean; }

    public HashMap<Integer, FisheryUserPowerUpBean> getPowerUpMap() { return powerUpMap; }

    public FisheryUserPowerUpBean getPowerUp(int powerUpId) { return powerUpMap.computeIfAbsent(powerUpId, k -> new FisheryUserPowerUpBean(getServerId(), userId, powerUpId, 0)); }

    public List<FisheryHourlyIncomeBean> getAllFishHourlyIncomeChanged() {
        return fisheryHourlyIncomeMap.values().stream()
                .filter(FisheryHourlyIncomeBean::checkChanged)
                .collect(Collectors.toList());
    }

    public long getFish() { return fish; }

    public long getCoins() { return coins - hiddenCoins; }

    public long getCoinsRaw() { return coins; }

    public int getRank() {
        try {
            return (int) (fisheryServerBean.getUsers().values().stream()
                    .filter(user -> (user.getFishIncome() > getFishIncome()) ||
                            (user.getFishIncome() == getFishIncome() && user.getFish() > getFish()) ||
                            (user.getFishIncome() == getFishIncome() && user.getFish() == getFish() && user.getCoins() > getCoins())
                    ).count() + 1);
        } catch (ConcurrentModificationException e) {
            LOGGER.error("Concurrent modification exception", e);
            return 0;
        }
    }

    public long getFishIncome() {
        Instant currentHourInstance = TimeUtil.instantRoundDownToHour(Instant.now());
        if (fishIncome == null || fishIncomeUpdateTime.isBefore(currentHourInstance)) {
            long n = 0;

            Instant effectiveInstant = currentHourInstance.minus(7, ChronoUnit.DAYS);
            for (Iterator<FisheryHourlyIncomeBean> iterator = fisheryHourlyIncomeMap.values().iterator(); iterator.hasNext(); ) {
                FisheryHourlyIncomeBean fisheryHourlyIncomeBean = iterator.next();
                if (fisheryHourlyIncomeBean.getTime().isBefore(effectiveInstant)) iterator.remove();
                else n += fisheryHourlyIncomeBean.getFishIncome();
            }

            fishIncome = n;
            fishIncomeUpdateTime = currentHourInstance;
            checkValuesBound();
        }

        return fishIncome;
    }

    private FisheryHourlyIncomeBean getCurrentFisheryHourlyIncome() {
        Instant currentTimeHour = TimeUtil.instantRoundDownToHour(Instant.now());
        return fisheryHourlyIncomeMap.computeIfAbsent(currentTimeHour, k -> new FisheryHourlyIncomeBean(getServerId(), userId, currentTimeHour, 0));
    }

    public LocalDate getDailyReceived() { return dailyReceived; }

    public int getDailyStreak() { return dailyStreak; }

    public int getUpvoteStack() { return upvoteStack; }

    public boolean isReminderSent() { return reminderSent; }

    public boolean isBanned() { return banned; }


    /* Setters */

    void setFisheryServerBean(FisheryServerBean fisheryServerBean) {
        if (this.fisheryServerBean == null) {
            this.fisheryServerBean = fisheryServerBean;
        }
    }

    public boolean registerMessage(Message message, ServerTextChannel channel) throws ExecutionException, InterruptedException {
        if (banned) return false;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);

        messagesThisHour++;
        if (messagesThisHour >= 3400) {
            banned = true;
            LOGGER.warn("### User temporarely banned with id " + userId);
            return false;
        }

        if (lastMessageHour != hour) {
            lastMessageHour = hour;
            messagesThisHour = 0;
        }

        if (!message.getContent().equalsIgnoreCase(lastContent)) {
            lastContent = message.getContent();
            int currentMessagePeriod = (Calendar.getInstance().get(Calendar.SECOND) + Calendar.getInstance().get(Calendar.MINUTE) * 60) / 20;
            if (currentMessagePeriod != lastMessagePeriod) {
                lastMessagePeriod = currentMessagePeriod;
                long effect = getPowerUp(FisheryCategoryInterface.PER_MESSAGE).getEffect();

                fish += effect;
                if (fishIncome != null) fishIncome += effect;
                getCurrentFisheryHourlyIncome().add(effect);
                checkValuesBound();
                setChanged();

                Optional<User> userOpt = getUser();
                if (fish >= 100 &&
                        !reminderSent &&
                        getServerBean().isFisheryReminders() &&
                        channel.canYouWrite() &&
                        channel.canYouEmbedLinks() &&
                        userOpt.isPresent()
                ) {
                    reminderSent = true;
                    User user = userOpt.get();
                    Locale locale = getServerBean().getLocale();
                    String prefix = getServerBean().getPrefix();

                    channel.sendMessage(user.getMentionTag(), EmbedFactory.getEmbed()
                            .setAuthor(user)
                            .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                            .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("%PREFIX", prefix))
                            .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("%PREFIX", prefix))).get();
                }

                return true;
            }
        }
        return false;
    }

    public void registerVC(int minutes) throws ExecutionException {
        if (!banned) {
            Optional<Integer> limitOpt = getServerBean().getFisheryVcHoursCap();
            if (limitOpt.isPresent() && ServerPatreonBoostCache.getInstance().get(getServerId()))
                minutes = Math.min(minutes, limitOpt.get() * 60 - vcMinutes);

            if (minutes > 0) {
                long effect = getPowerUp(FisheryCategoryInterface.PER_VC).getEffect() * minutes;

                fish += effect;
                if (fishIncome != null) fishIncome += effect;
                getCurrentFisheryHourlyIncome().add(effect);
                vcMinutes += minutes;

                checkValuesBound();
                setChanged();
            }
        }
    }

    public EmbedBuilder getAccountEmbed() {
        return changeValues(0, 0);
    }

    public EmbedBuilder changeValues(long fishAdd, long coinsAdd) {
        return changeValues(fishAdd, coinsAdd, null);
    }

    public void setFish(long fish) {
        if (this.fish != fish) {
            this.fish = fish;
            checkValuesBound();
            setChanged();
        }
    }

    public void setCoins(long coins) {
        if (this.coins != coins) {
            this.coins = coins;
            checkValuesBound();
            setChanged();
        }
    }

    public void setDailyStreak(int dailyStreak) {
        if (this.dailyStreak != dailyStreak) {
            this.dailyStreak = dailyStreak;
            checkValuesBound();
            setChanged();
        }
    }

    public synchronized EmbedBuilder changeValues(long fishAdd, long coinsAdd, Integer newDailyStreak) {
        /* Collect Current Data */
        long fishIncomePrevious = getFishIncome();
        long fishPrevious = getFish();
        long coinsPrevious = getCoins();
        long rankPrevious = getRank();
        int dailyStreakPrevious = getDailyStreak();

        /* Update Changes */
        if (fishAdd != 0) {
            fish += fishAdd;
            if (fishAdd > 0) {
                if (fishIncome != null) fishIncome += fishAdd;
                getCurrentFisheryHourlyIncome().add(fishAdd);
            }
        }
        if (coinsAdd != 0) coins += coinsAdd;
        if (newDailyStreak != null) dailyStreak = newDailyStreak;
        checkValuesBound();
        setChanged();

        long rank = getRank();
        long fishIncome = getFishIncome();

        /* Generate Account Embed */
        Server server = getServer().get();
        User user = server.getMemberById(userId).orElse(null);
        Locale locale = getServerBean().getLocale();

        if (user == null) return null;

        boolean patron = false;
        try {
            patron = PatreonCache.getInstance().getPatreonLevel(userId) >= 1;
        } catch (ExecutionException throwables) {
            LOGGER.error("Error in donation check", throwables);
        }

        String patreonEmoji = "\uD83D\uDC51";
        String displayName = user.getDisplayName(server);
        while (displayName.length() > 0 && displayName.startsWith(patreonEmoji)) displayName = displayName.substring(patreonEmoji.length());

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setAuthor(TextManager.getString(locale, TextManager.GENERAL, "rankingprogress_title", patron, displayName, patreonEmoji), "", user.getAvatar())
                .setThumbnail(user.getAvatar());

        if (patron) eb.setColor(Color.YELLOW);
        if (fishAdd > 0 || (fishAdd == 0 && coinsAdd > 0))
            eb.setColor(Color.GREEN);
        else if (coinsAdd <= 0 && (fishAdd < 0 || coinsAdd < 0))
            eb.setColor(Color.RED);

        String codeBlock = CodeBlockColor.WHITE;
        if (rank < rankPrevious)
            codeBlock = CodeBlockColor.GREEN;
        else if (rank > rankPrevious)
            codeBlock = CodeBlockColor.RED;

        eb.setDescription(TextManager.getString(locale, TextManager.GENERAL, coins != 0 ? "rankingprogress_desription" : "rankingprogress_desription_nocoins",
                getEmbedSlot(locale, fishIncome, fishIncomePrevious, false),
                getEmbedSlot(locale, getFish(), fishPrevious, false),
                getEmbedSlot(locale, getCoins(), coinsPrevious, false),
                getEmbedSlot(locale, getDailyStreak(), newDailyStreak != null ? dailyStreakPrevious : getDailyStreak(), false),
                getEmbedSlot(locale, rank, rankPrevious, true),
                codeBlock
        ));

        if (banned) EmbedFactory.addLog(eb, LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "banned"));

        return eb;
    }

    private String getEmbedSlot(Locale locale, long numberNow, long numberPrevious, boolean rankSlot) {
        long diff = numberNow - numberPrevious;
        String diffSign = diff >= 0 ? "+" : "";
        return TextManager.getString(locale, TextManager.GENERAL, rankSlot ? "rankingprogress_update2" : "rankingprogress_update", diff != 0,
                StringUtil.numToString(locale, numberPrevious),
                StringUtil.numToString(locale, numberNow),
                diffSign + StringUtil.numToString(locale, diff)
        );
    }

    private void checkValuesBound() {
        if (fish > Settings.MAX) fish = Settings.MAX;
        else if (fish < 0) fish = 0;

        if (coins > Settings.MAX) coins = Settings.MAX;
        else if (coins < 0) coins = 0;

        if (fishIncome != null) {
            if (fishIncome > Settings.MAX) fishIncome = Settings.MAX;
            else if (fishIncome < 0) fishIncome = 0L;
        }

        if (dailyStreak < 0) dailyStreak = 0;
    }

    public void levelUp(int powerUpId) {
        getPowerUp(powerUpId).levelUp();
        setChanged();
    }

    public void updateDailyReceived() {
        if (!LocalDate.now().equals(dailyReceived)) {
            dailyReceived = LocalDate.now();
            checkValuesBound();
            setChanged();
        }
    }

    public void increaseDailyStreak() {
        dailyStreak++;
        setChanged();
    }

    public void resetDailyStreak() {
        dailyStreak = 0;
        setChanged();
    }

    public void addUpvote(int upvotes) {
        if (upvotes > 0) {
            upvoteStack += upvotes;
            setChanged();
        }
    }

    public void addHiddenCoins(long amount) {
        hiddenCoins = Math.min(coins, hiddenCoins + amount);
    }

    public void clearUpvoteStack() {
        if (upvoteStack > 0) {
            upvoteStack = 0;
            setChanged();
        }
    }

    public void setReminderSent() {
        if (!reminderSent) {
            reminderSent = true;
            setChanged();
        }
    }

    public boolean checkChanged() {
        boolean changedTemp = changed;
        changed = false;
        return changedTemp;
    }

    public void setChanged() {
        fisheryServerBean.update();
        changed = true;
    }

}