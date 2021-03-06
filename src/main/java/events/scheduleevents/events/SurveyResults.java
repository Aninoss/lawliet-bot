package events.scheduleevents.events;

import constants.Category;
import constants.FisheryCategoryInterface;
import constants.FisheryStatus;
import core.*;
import core.schedule.ScheduleInterface;
import core.utils.JDAUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryUserBean;
import mysql.modules.server.DBServer;
import mysql.modules.survey.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

@ScheduleEventFixedRate(rateValue = 10, rateUnit = ChronoUnit.MINUTES)
public class SurveyResults implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Bot.isProductionMode()) {
            SurveyBean surveyBean = DBSurvey.getInstance().getCurrentSurvey();
            LocalDate today = LocalDate.now();
            if (!today.isBefore(surveyBean.getNextDate()) && ShardManager.getInstance().isEverythingConnected()) {
                processCurrentResults();
            }
        }
    }
    
    public static void processCurrentResults() {
        GlobalThreadPool.getExecutorService().submit(() -> {
            DBSurvey.getInstance().clear();
            SurveyBean lastSurvey = DBSurvey.getInstance().getCurrentSurvey();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {}
            DBSurvey.getInstance().next();

            MainLogger.get().info("Calculating survey results...");
            processSurvey(lastSurvey);
        });
    }

    private static void processSurvey(SurveyBean lastSurvey) {
        byte won = lastSurvey.getWon();
        int percent = won != 2 ? (int) Math.round(lastSurvey.getFirstVoteNumbers(won) / (double) lastSurvey.getFirstVoteNumber() * 100) : 0;

        /* Group each second vote into a specific group for each user */
        HashMap<Long, ArrayList<SurveySecondVote>> secondVotesMap = new HashMap<>();
        for (SurveySecondVote surveySecondVote : lastSurvey.getSecondVotes().values()) {
            if (surveySecondVote.getServer().isPresent() &&
                    DBServer.getInstance().getBean(surveySecondVote.getServerId()).getFisheryStatus() == FisheryStatus.ACTIVE
            ) {
                secondVotesMap.computeIfAbsent(surveySecondVote.getUserId(), k -> new ArrayList<>()).add(surveySecondVote);
            }
        }

        MainLogger.get().info("Survey giving out prices for {} users", secondVotesMap.keySet().size());
        ArrayList<Long> notificationUsers = Bot.getClusterId() == 1 ? new ArrayList<>(lastSurvey.getNotificationUserIds()) : new ArrayList<>();
        for (long userId : secondVotesMap.keySet()) {
            try {
                ShardManager.getInstance().getCachedUserById(userId).ifPresent(user -> {
                    try {
                        MainLogger.get().info("### SURVEY MANAGE USER {} ###", user.getName());
                        processSurveyUser(secondVotesMap.get(userId), user, won);
                        if (notificationUsers.contains(userId)) {
                            notificationUsers.remove(userId);
                            sendSurveyResult(lastSurvey, user, won, percent);
                            Thread.sleep(100);
                        }
                    } catch (Throwable e) {
                        MainLogger.get().error("Exception while managing user {}", userId, e);
                    }
                });
            } catch (Throwable e) {
                MainLogger.get().error("Exception while managing user {}", userId, e);
            }
        }

        notificationUsers.forEach(userId -> {
            ShardManager.getInstance().fetchUserById(userId).join().ifPresent(user -> {
                try {
                    sendSurveyResult(lastSurvey, user, won, percent);
                    Thread.sleep(100);
                } catch (Throwable e) {
                    MainLogger.get().error("Exception while managing user {}", userId, e);
                }
            });
        });

        MainLogger.get().info("Survey results finished");
    }

    private static void processSurveyUser(ArrayList<SurveySecondVote> secondVotes, User user, byte won) {
        secondVotes.stream()
                .filter(secondVote -> won == 2 || secondVote.getVote() == won)
                .forEach(secondVote -> {
                    FisheryUserBean userBean = DBFishery.getInstance().getBean(secondVote.getServerId()).getUserBean(user.getIdLong());
                    long price = userBean.getPowerUp(FisheryCategoryInterface.PER_SURVEY).getEffect();
                    userBean.changeValues(0, price);
                });
    }

    private static void sendSurveyResult(SurveyBean lastSurvey, User user, byte won, int percent) throws IOException {
        SurveyFirstVote surveyFirstVote = lastSurvey.getFirstVotes().get(user.getIdLong());
        if (surveyFirstVote != null) {
            Locale locale = surveyFirstVote.getLocale();
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

            JDAUtil.sendPrivateMessage(user, eb.build()).queue();
        }
    }

}
