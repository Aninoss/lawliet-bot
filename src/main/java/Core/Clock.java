package Core;

import Constants.*;
import Core.Utils.SystemUtil;
import Modules.Porn.PornImageCache;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Core.Utils.StringUtil;
import Core.Utils.TimeUtil;
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
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class Clock {

    private static final Clock ourInstance = new Clock();
    public static Clock getInstance() { return ourInstance; }
    private Clock() {}

    final Logger LOGGER = LoggerFactory.getLogger(Clock.class);
    private boolean trafficWarned = false;
    private boolean readyForRestart = false;

    public void start() {
        LOGGER.info("Starting Clock");

        //Start 10 Minutes Event Loop
        Thread t = new CustomThread(() -> {
            try {
                while (true) {
                    Thread.sleep(1000 * 60 * 10);
                    try {
                        every10Minutes();
                    } catch (Exception e) {
                        LOGGER.error("Error in 10 min clock", e);
                    }
                }
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted", e);
            }
        }, "clock_10min");
        t.start();

        try {
            while (true) {
                Thread.sleep(TimeUtil.getMilisBetweenInstants(Instant.now(), TimeUtil.setInstantToNextHour(Instant.now())));
                try {
                    onHourStart();
                } catch (Exception e) {
                    LOGGER.error("Error in hourly clock", e);
                }
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
        }
    }

    private void onHourStart() {
        //Reset Patreon Cache
        PatreonCache.getInstance().reset();

        //New Day
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) == 0) {
            try {
                onDayStart();
            } catch (Exception e) {
                LOGGER.error("Exception in daily method", e);
            }
        }

        //Survey Results
        try {
            SurveyBean surveyBean = DBSurvey.getInstance().getCurrentSurvey();
            LocalDate today = LocalDate.now();
            if (!today.isBefore(surveyBean.getNextDate())) {
                try {
                    LOGGER.info("Calculating survey results...");
                    updateSurvey();
                } catch (Exception e) {
                    LOGGER.error("Could not update survey", e);
                }
            }
        } catch (ExecutionException | SQLException e) {
            LOGGER.error("Exception while fetching survey bean", e);
        }
    }

    private void onDayStart() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        try {
            trafficWarned = false; //Reset Traffic Warning
            SubredditContainer.getInstance().reset(); //Resets Subreddit Cache
            RunningCommandManager.getInstance().clear(); //Resets Running Commands
            PornImageCache.getInstance().reset(); //Resets Porn Cache
            DBUpvotes.getInstance().cleanUp(); //Cleans Up Bot Upvote List
            ServerPatreonBoostCache.getInstance().reset(); //Resets server patreon boost cache
            PatreonCache.getInstance().reset(); //Resets patreon cache
        } catch (Exception e) {
            LOGGER.error("Exception while resetting bot", e);
        }

        //Send Bot Stats
        try {
            DBBotStats.addStatCommandUsages();
        } catch (Exception e) {
            LOGGER.error("Could not post command usages stats", e);
        }
        try {
            DBBotStats.addStatServers(apiCollection.getServerTotalSize());
        } catch (Exception e) {
            LOGGER.error("Could not post total server count", e);
        }
        try {
            DBBotStats.addStatUpvotes();
        } catch (Exception e) {
            LOGGER.error("Could not post upvotes stats", e);
        }
    }


    private void every10Minutes() {
        DiscordApiCollection apiCollection = DiscordApiCollection.getInstance();

        if (!apiCollection.allShardsConnected())
            LOGGER.error("At least 1 shard is offline");

        //Cleans Cooldown List
        try {
            Cooldown.getInstance().clean();
        } catch (Exception e) {
            LOGGER.error("Exception while cleaning cooldown list", e);
        }

        //Analyzes Traffic
        try {
            double trafficGB = SIGNALTRANSMITTER.getInstance().getTrafficGB();
            Console.getInstance().setTraffic(trafficGB);

            if (trafficGB >= 95 && !trafficWarned) {
                try {
                    apiCollection.getOwner().sendMessage("Traffic Warning! " + trafficGB + " GB!").get();
                } catch (ExecutionException e) {
                    LOGGER.error("Could not send message", e);
                }
                trafficWarned = true;
            }

            if (trafficGB >= 120) {
                LOGGER.error("EXIT - Too much traffic");
                System.exit(-1);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while checking traffic", e);
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
        try {
            Connector.updateActivity();
        } catch (Exception e) {
            LOGGER.error("Error while updating activity", e);
        }

        //Updates Discord Bots Server Count
        try {
            if (Bot.isProductionMode() && apiCollection.allShardsConnected()) {
                int totalServers = apiCollection.getServerTotalSize();

                TopGG.getInstance().updateServerCount(totalServers);
                Botsfordiscord.updateServerCount(totalServers);
                BotsOnDiscord.updateServerCount(totalServers);
                Discordbotlist.updateServerCount(totalServers);
                Discordbotsgg.updateServerCount(totalServers);
            }
        } catch (Exception e) {
            LOGGER.error("Error while updating bots server count", e);
        }

        //Restart All Shards at 07:xx AM
        try {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);

            if (hour == Settings.UPDATE_HOUR && readyForRestart) {
                readyForRestart = false;
                LOGGER.info("Backup database...");
                try {
                    SystemUtil.backupDB();
                } catch (Exception e) {
                    LOGGER.error("Error while creating database backup", e);
                }

                /* Posting daily unique users stats */
                if (DiscordApiCollection.getInstance().getStartingTime().isBefore(Instant.now().minus(23, ChronoUnit.HOURS))) {
                    try {
                        DBBotStats.addStatUniqueUsers();
                    } catch (Exception e) {
                        LOGGER.error("Could not post unique users stats", e);
                    }
                }

                if (Bot.hasUpdate()) {
                    LOGGER.info("EXIT - Restarting for update...");
                    System.exit(0);
                } else {
                    DBMain.getInstance().clearCache();
                    LOGGER.info("Cache cleaned successfully");
                }
            } else if (hour < Settings.UPDATE_HOUR) {
                readyForRestart = true;
            }
        } catch (Exception e) {
            LOGGER.error("Error while looking for bot updates", e);
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
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                LOGGER.error("Exception while initializing user list for fishery survey", e);
            }
        }

        LOGGER.info("Survey giving out prices for {} users", secondVotesMap.keySet().size());

        try {
            for (long userId : secondVotesMap.keySet()) {
                try {
                    User user = DiscordApiCollection.getInstance().getUserById(userId).orElse(null);
                    if (user != null) {
                        LOGGER.info("### SURVEY MANAGE USER {} ###", user.getName());
                        manageSurveyUser(lastSurvey, secondVotesMap.get(userId), user, won, percent);
                    }
                } catch (Exception e) {
                    LOGGER.error("Exception while managing user {}", userId, e);
                }
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
        }

        LOGGER.info("Survey results finished");
    }

    private void manageSurveyUser(SurveyBean lastSurvey, ArrayList<SurveySecondVote> secondVotes, User user, byte won, int percent) throws IOException, InterruptedException, ExecutionException {
        Locale localeGerman = new Locale(Locales.DE);

        HashMap<Long, Long> coinsWinMap = new HashMap<>();
        secondVotes.stream()
                .filter(secondVote -> won == 2 || secondVote.getVote() == won)
                .forEach(secondVote -> {
                    try {
                        Server server = DiscordApiCollection.getInstance().getServerById(secondVote.getServerId()).get();
                        FisheryUserBean userBean = DBFishery.getInstance().getBean(server.getId()).getUserBean(user.getId());
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
                .setTitle(TextManager.getString(locale, Category.FISHERY, "survey_results_message_title"))
                .setDescription(TextManager.getString(locale, Category.FISHERY, "survey_results_message_template", won == 2,
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
                        sb.append(TextManager.getString(locale, Category.FISHERY, "survey_results_message_server",
                                finalI,
                                DiscordApiCollection.getInstance().getServerById(secondVote.getServerId()).get().getName(),
                                StringUtil.numToString(locale, coinsWinMap.computeIfAbsent(secondVote.getServerId(), k -> 0L))
                        )).append("\n");
                    });

            if (sb.length() > 0)
                eb.addField(TextManager.getString(locale, Category.FISHERY, "survey_results_message_wonlost", i), sb.toString());
        }

        user.sendMessage(eb).get();
    }

}
