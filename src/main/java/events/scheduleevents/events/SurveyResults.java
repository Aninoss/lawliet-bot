package events.scheduleevents.events;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import constants.Category;
import constants.FisheryGear;
import constants.FisheryStatus;
import core.*;
import core.schedule.ScheduleInterface;
import core.utils.JDAUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import mysql.modules.guild.DBGuild;
import mysql.modules.survey.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;

@ScheduleEventFixedRate(rateValue = 10, rateUnit = ChronoUnit.MINUTES)
public class SurveyResults implements ScheduleInterface {

    @Override
    public void run() throws Throwable {
        if (Program.isProductionMode()) {
            SurveyData surveyData = DBSurvey.getInstance().getCurrentSurvey();
            LocalDate today = LocalDate.now();
            if (!today.isBefore(surveyData.getNextDate()) && ShardManager.getInstance().isEverythingConnected()) {
                processCurrentResults();
            }
        }
    }

    public static void processCurrentResults() {
        GlobalThreadPool.getExecutorService().submit(() -> {
            DBSurvey.getInstance().clear();
            SurveyData lastSurvey = DBSurvey.getInstance().getCurrentSurvey();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
            }
            DBSurvey.getInstance().next();

            MainLogger.get().info("Calculating survey results...");
            processSurvey(lastSurvey);
        });
    }

    private static void processSurvey(SurveyData lastSurvey) {
        byte won = lastSurvey.getWon();
        int percent = won != 2 ? (int) Math.round(lastSurvey.getFirstVoteNumbers(won) / (double) lastSurvey.getFirstVoteNumber() * 100) : 0;

        /* Group each second vote into a specific group for each user */
        HashMap<Long, ArrayList<SurveySecondVote>> secondVotesMap = new HashMap<>();
        for (SurveySecondVote surveySecondVote : lastSurvey.getSecondVotes().values()) {
            if (surveySecondVote.getGuild().isPresent() &&
                    DBGuild.getInstance().retrieve(surveySecondVote.getGuildId()).getFisheryStatus() == FisheryStatus.ACTIVE
            ) {
                secondVotesMap.computeIfAbsent(surveySecondVote.getMemberId(), k -> new ArrayList<>()).add(surveySecondVote);
            }
        }

        MainLogger.get().info("Survey giving out prices for {} users", secondVotesMap.keySet().size());
        ArrayList<Long> notificationUsers = Program.getClusterId() == 1 ? new ArrayList<>(lastSurvey.getNotificationUserIds()) : new ArrayList<>();
        for (long userId : secondVotesMap.keySet()) {
            try {
                ShardManager.getInstance().getCachedUserById(userId).ifPresent(user -> {
                    try {
                        MainLogger.get().info("### SURVEY MANAGE USER {} ###", user.getName());
                        processSurveyUser(secondVotesMap.get(userId), user, won);
                        if (notificationUsers.contains(userId)) {
                            notificationUsers.remove(userId);
                            sendSurveyResult(lastSurvey, user.getIdLong(), won, percent);
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
            try {
                sendSurveyResult(lastSurvey, userId, won, percent);
            } catch (Throwable e) {
                MainLogger.get().error("Exception while managing user {}", userId, e);
            }
        });

        MainLogger.get().info("Survey results finished");
    }

    private static void processSurveyUser(ArrayList<SurveySecondVote> secondVotes, User user, byte won) {
        secondVotes.stream()
                .filter(secondVote -> won == 2 || secondVote.getVote() == won)
                .forEach(secondVote -> {
                    FisheryMemberData userBean = DBFishery.getInstance().retrieve(secondVote.getGuildId()).getMemberBean(user.getIdLong());
                    long price = userBean.getMemberGear(FisheryGear.SURVEY).getEffect();
                    MainLogger.get().info("Survey: Giving {} coins to {} ({})", price, user, user.getIdLong());
                    userBean.changeValues(0, price);
                });
    }

    private static void sendSurveyResult(SurveyData lastSurvey, long userId, byte won, int percent) throws IOException {
        SurveyFirstVote surveyFirstVote = lastSurvey.getFirstVotes().get(userId);
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

            JDAUtil.sendPrivateMessage(userId, eb.build()).queue();
        }
    }

}
