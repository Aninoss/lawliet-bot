package Core;

import Constants.FisheryStatus;
import Constants.FisheryCategoryInterface;
import Constants.Locales;
import Constants.Settings;
import Modules.Porn.PornImageCache;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.Tools.StringTools;
import Core.Tools.TimeTools;
import MySQL.*;
import MySQL.Modules.FisheryUsers.DBFishery;
import MySQL.Modules.FisheryUsers.FisheryUserBean;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Survey.DBSurvey;
import MySQL.Modules.Survey.SurveyBean;
import MySQL.Modules.Survey.SurveyQuestion;
import MySQL.Modules.Survey.SurveySecondVote;
import MySQL.Modules.Upvotes.DBUpvotes;
import ServerStuff.*;
import CommandSupporters.Cooldown.Cooldown;
import Modules.Reddit.SubredditContainer;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Clock {

    private static final Clock ourInstance = new Clock();
    public static Clock getInstance() { return ourInstance; }
    private Clock() { start(); }

    final static Logger LOGGER = LoggerFactory.getLogger(Clock.class);
    private boolean trafficWarned = false;
    private boolean readyForRestart = false;

    private void start() {
        //Start 10 Minutes Event Loop
        Thread t = new CustomThread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000 * 60 * 10);
                    every10Minutes();
                } catch (InterruptedException e) {
                    LOGGER.error("Interrupted", e);
                }
            }
        }, "clock_10min");
        t.start();

        try {
            while (true) {
                Thread.sleep(TimeTools.getMilisBetweenInstants(Instant.now(), TimeTools.setInstantToNextHour(Instant.now())));
                onHourStart();
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
        }
    }

    private void onHourStart() {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) == 0) {
            onDayStart();
        }
    }

    private void onDayStart() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        trafficWarned = false; //Reset Traffic Warning
        SubredditContainer.getInstance().reset(); //Resets Subreddit Cache
        RunningCommandManager.getInstance().clear(); //Resets Running Commands
        PornImageCache.getInstance().reset(); //Resets Porn Cache
        DBUpvotes.getInstance().cleanUp(); //Cleans Up Bot Upvote List

        DonationHandler.checkExpiredDonations(); //Check Expired Donations

        //Send Bot Stats
        try {
            DBBotStats.addStatCommandUsages();
        } catch (SQLException e) {
            LOGGER.error("Could not post command usages stats", e);
        }
        try {
            DBBotStats.addStatServers(apiCollection.getServerTotalSize());
        } catch (SQLException e) {
            LOGGER.error("Could not post total server count", e);
        }
        try {
            DBBotStats.addStatUpvotes();
        } catch (SQLException | ExecutionException | InterruptedException e) {
            LOGGER.error("Could not post upvotes stats", e);
        }

        //Survey Results
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.MONDAY || day == Calendar.THURSDAY) {
            try {
                updateSurvey();
            } catch (SQLException | ExecutionException e) {
                LOGGER.error("Could not update survey", e);
            }
        }
    }


    private void every10Minutes() throws InterruptedException {
        if (!DiscordApiCollection.getInstance().allShardsConnected())
            LOGGER.error("At least 1 shard is offline");

        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        //Cleans Cooldown List
        Cooldown.getInstance().clean();

        //Analyzes Traffic
        double trafficGB = SIGNALTRANSMITTER.getInstance().getTrafficGB();
        Console.getInstance().setTraffic(trafficGB);

        if (trafficGB >= 60 && !trafficWarned) {
            try {
                apiCollection.getOwner().sendMessage("Traffic Warning! " + trafficGB + " GB!").get();
            } catch (ExecutionException e) {
                LOGGER.error("Could not send message", e);
            }
            trafficWarned = true;
        }

        if (trafficGB >= 100) {
            LOGGER.error("Too much traffic");
            System.exit(-1);
        }

        //Checks Database Connection
        if (!DBMain.getInstance().checkConnection()) {
            try {
                LOGGER.error("Database disconnected! Trying to reconnect");
                DBMain.getInstance().connect();
            } catch (IOException | SQLException e) {
                LOGGER.error("Could not connect with database", e);
            }
        }

        //Updates Activity
        Connector.updateActivity();

        //Updates Discord Bots Server Count
        if (Bot.isProductionMode() && apiCollection.allShardsConnected()) {
            int totalServers = apiCollection.getServerTotalSize();

            TopGG.getInstance().updateServerCount(totalServers);
            Botsfordiscord.updateServerCount(totalServers);
            BotsOnDiscord.updateServerCount(totalServers);
            Discordbotlist.updateServerCount(totalServers);
            Divinediscordbots.updateServerCount(totalServers);
            Discordbotsgg.updateServerCount(totalServers);
        }

        //Updates survey manually
        File surveyCheckFile = new File("survey_update");
        if (surveyCheckFile.exists()) {
            if (surveyCheckFile.delete()) {
                LOGGER.info("Manual survey update");
                try {
                    updateSurvey();
                } catch (SQLException | ExecutionException e) {
                    LOGGER.error("Could not update survey", e);
                }
            }
        }

        //Restart All Shards at 09:xx AM
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour == Settings.UPDATE_HOUR && readyForRestart) {
            readyForRestart = false;

            if (Bot.hasUpdate()) {
                LOGGER.info("Restarting for update...");
                System.exit(0);
            } else {
                DBMain.getInstance().clearCache();
                LOGGER.info("Cache cleaned successfully");
            }
        } else if (hour < Settings.UPDATE_HOUR) {
            readyForRestart = true;
        }
    }

    private void updateSurvey() throws SQLException, ExecutionException {
        DiscordApiCollection.getInstance().waitForStartup();
        SurveyBean lastSurvey = DBSurvey.getInstance().getCurrentSurvey();
        DBSurvey.getInstance().next();

        byte won = lastSurvey.getWon();
        int percent = 0;
        if (won != 2)
            percent = (int) Math.round(lastSurvey.getFirstVoteNumbers(won) / (double) lastSurvey.getFirstVoteNumber() * 100);

        /* Group each second vote into a specific group for each user */
        HashMap<Long, ArrayList<SurveySecondVote>> secondVotesMap = new HashMap<>();
        for (SurveySecondVote surveySecondVote : lastSurvey.getSecondVotes().values()) {
            try {
                if (DiscordApiCollection.getInstance().getServerById(surveySecondVote.getServerId()).isPresent() &&
                        DBServer.getInstance().getBean(surveySecondVote.getServerId()).getFisheryStatus() == FisheryStatus.ACTIVE) {
                    LOGGER.debug("Enter user ID {}", surveySecondVote.getUserId());
                    secondVotesMap.computeIfAbsent(surveySecondVote.getUserId(), k -> new ArrayList<>()).add(surveySecondVote);
                    Thread.sleep(1000);
                }
            } catch (Throwable e) {
                LOGGER.error("Exception", e);
            }
        }

        try {
            for (long userId : secondVotesMap.keySet()) {
                try {
                    User user = DiscordApiCollection.getInstance().getUserById(userId).orElse(null);
                    if (user != null) {
                        LOGGER.debug("Manage survey user {}", user.getName());
                        manageSurveyUser(lastSurvey, secondVotesMap.get(userId), user, won, percent);
                    }
                } catch (Throwable e) {
                    LOGGER.error("Exception", e);
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
        }
    }

    private void manageSurveyUser(SurveyBean lastSurvey, ArrayList<SurveySecondVote> secondVotes, User user, byte won, int percent) throws Throwable {
        Locale localeGerman = new Locale(Locales.DE);

        HashMap<Long, Long> coinsWinMap = new HashMap<>();
        secondVotes.stream()
                .filter(secondVote -> won == 2 || secondVote.getVote() == won)
                .forEach(secondVote -> {
                    try {
                        Server server = DiscordApiCollection.getInstance().getServerById(secondVote.getServerId()).get();
                        FisheryUserBean userBean = DBFishery.getInstance().getBean(server.getId()).getUser(user.getId());
                        long price = userBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect();
                        userBean.changeValues(0, price);
                        coinsWinMap.put(secondVote.getServerId(), price);
                    } catch (ExecutionException e) {
                        LOGGER.error("Exception", e);
                    }
                });

        boolean prefersGerman = secondVotes.stream()
                .filter(secondVote -> {
                    try {
                        return DBServer.getInstance().getBean(secondVote.getServerId()).getLocale().equals(localeGerman);
                    } catch (ExecutionException e) {
                        LOGGER.error("Could not get server bean", e);
                    }
                    return false;
                })
                .count() >= secondVotes.size() / 2.0;

        Locale locale = new Locale(prefersGerman ? Locales.DE : Locales.EN);
        SurveyQuestion surveyQuestion = lastSurvey.getSurveyQuestionAndAnswers(locale);

        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_title"))
                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_template", won == 2,
                        surveyQuestion.getQuestion(),
                        surveyQuestion.getAnswers()[0],
                        surveyQuestion.getAnswers()[1],
                        surveyQuestion.getAnswers()[Math.min(1, won)].toUpperCase(),
                        String.valueOf(percent)
                ));

        for(int i = 0; i < 2; i++) {
            StringBuilder sb = new StringBuilder();
            int finalI = i;
            secondVotes.stream()
                    .filter(secondVote -> (finalI == 0) == (won == 2 || secondVote.getVote() == won))
                    .forEach(secondVote -> {
                        sb.append(TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_server",
                                finalI,
                                DiscordApiCollection.getInstance().getServerById(secondVote.getServerId()).get().getName(),
                                StringTools.numToString(locale, coinsWinMap.computeIfAbsent(secondVote.getServerId(), k -> 0L))
                        )).append("\n");
                    });

            if (sb.length() > 0)
                eb.addField(TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_wonlost", i), sb.toString());
        }

        user.sendMessage(eb).get();
    }

}
