package events.scheduleevents.events;

import constants.Category;
import constants.FisheryCategoryInterface;
import constants.FisheryStatus;
import constants.Locales;
import core.*;
import core.schedule.ScheduleInterface;
import core.utils.StringUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import mysql.modules.survey.DBSurvey;
import mysql.modules.survey.SurveyBean;
import mysql.modules.survey.SurveyQuestion;
import mysql.modules.survey.SurveySecondVote;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@ScheduleEventFixedRate(rateValue = 10, rateUnit = ChronoUnit.MINUTES)
public class SurveyResults implements ScheduleInterface {

    final Logger LOGGER = LoggerFactory.getLogger(SurveyResults.class);

    /* TODO adjust for clustering */
    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            SurveyBean surveyBean = DBSurvey.getInstance().getCurrentSurvey();
            LocalDate today = LocalDate.now();
            if (!today.isBefore(surveyBean.getNextDate())) {
                new CustomThread(() -> {
                    try {
                        LOGGER.info("Calculating survey results...");
                        updateSurvey();
                    } catch (Throwable e) {
                        LOGGER.error("Could not update survey", e);
                    }
                }, "survey_results").start();
            }
        }
    }

    private void updateSurvey() throws SQLException, ExecutionException {
        if (!DiscordApiManager.getInstance().isEverythingConnected())
            return;

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
                if (DiscordApiManager.getInstance().getLocalServerById(surveySecondVote.getServerId()).isPresent() &&
                        DBServer.getInstance().getBean(surveySecondVote.getServerId()).getFisheryStatus() == FisheryStatus.ACTIVE) {
                    LOGGER.debug("Enter user ID {}", surveySecondVote.getUserId());
                    secondVotesMap.computeIfAbsent(surveySecondVote.getUserId(), k -> new ArrayList<>()).add(surveySecondVote);
                }
            } catch (Throwable e) {
                LOGGER.error("Exception while initializing user list for fishery survey", e);
            }
        }

        LOGGER.info("Survey giving out prices for {} users", secondVotesMap.keySet().size());

        for (long userId : secondVotesMap.keySet()) {
            try {
                int finalPercent = percent;
                DiscordApiManager.getInstance().fetchUserById(userId).get().ifPresent(user -> {
                    try {
                        LOGGER.info("### SURVEY MANAGE USER {} ###", user.getName());
                        manageSurveyUser(lastSurvey, secondVotesMap.get(userId), user, won, finalPercent);
                        Thread.sleep(20);
                    } catch (Throwable e) {
                        LOGGER.error("Exception while managing user {}", userId, e);
                    }
                });
            } catch (Throwable e) {
                LOGGER.error("Exception while managing user {}", userId, e);
            }
        }

        LOGGER.info("Survey results finished");
    }

    private void manageSurveyUser(SurveyBean lastSurvey, ArrayList<SurveySecondVote> secondVotes, User user, byte won, int percent) throws IOException {
        Locale localeGerman = new Locale(Locales.DE);

        HashMap<Long, Long> coinsWinMap = new HashMap<>();
        secondVotes.stream()
                .filter(secondVote -> won == 2 || secondVote.getVote() == won)
                .forEach(secondVote -> {
                    Server server = DiscordApiManager.getInstance().getLocalServerById(secondVote.getServerId()).get();
                    FisheryUserBean userBean = DBFishery.getInstance().getBean(server.getId()).getUserBean(user.getId());
                    long price = userBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect();
                    userBean.changeValues(0, price);
                    coinsWinMap.put(secondVote.getServerId(), price);
                });

        boolean prefersGerman = secondVotes.stream()
                .filter(secondVote -> DBServer.getInstance().getBean(secondVote.getServerId()).getLocale().equals(localeGerman))
                .count() >= secondVotes.size() / 2.0;

        Locale locale = new Locale(prefersGerman ? Locales.DE : Locales.EN);
        SurveyQuestion surveyQuestion = lastSurvey.getSurveyQuestionAndAnswers(locale);

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(locale, Category.FISHERY, "survey_results_message_title"))
                .setDescription(TextManager.getString(locale, Category.FISHERY, "survey_results_message_template", won == 2,
                        surveyQuestion.getQuestion(),
                        surveyQuestion.getAnswers()[0],
                        surveyQuestion.getAnswers()[1],
                        surveyQuestion.getAnswers()[Math.min(1, won)].toUpperCase(),
                        String.valueOf(percent)
                ));

        for (int i = 0; i < 2; i++) {
            StringBuilder sb = new StringBuilder();
            int finalI = i;
            secondVotes.stream()
                    .filter(secondVote -> (finalI == 0) == (won == 2 || secondVote.getVote() == won))
                    .forEach(secondVote -> {
                        sb.append(TextManager.getString(locale, Category.FISHERY, "survey_results_message_server",
                                finalI,
                                StringUtil.escapeMarkdown(DiscordApiManager.getInstance().getLocalServerById(secondVote.getServerId()).get().getName()),
                                StringUtil.numToString(coinsWinMap.computeIfAbsent(secondVote.getServerId(), k -> 0L))
                        )).append("\n");
                    });

            if (sb.length() > 0)
                eb.addField(TextManager.getString(locale, Category.FISHERY, "survey_results_message_wonlost", i), sb.toString());
        }

        if (lastSurvey.hasNotificationUserId(user.getId())) {
            user.sendMessage(eb).exceptionally(ExceptionLogger.get());
        }
    }

}
