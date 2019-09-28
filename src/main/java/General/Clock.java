package General;

import Commands.PowerPlant.SellCommand;
import Commands.PowerPlant.SurveyCommand;
import Constants.FishingCategoryInterface;
import Constants.Settings;
import General.RunningCommands.RunningCommandManager;
import MySQL.*;
import ServerStuff.DiscordBotsAPI.DiscordbotsAPI;
import ServerStuff.Donations.DonationServer;
import GUIPackage.GUI;
import General.Cooldown.Cooldown;
import General.Reddit.SubredditContainer;
import General.Survey.*;
import General.Tracker.TrackerManager;
import ServerStuff.SIGNALTRANSMITTER.SIGNALTRANSMITTER;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.entity.server.Server;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class Clock {
    private static boolean trafficWarned = false;

    public static void tick(DiscordApi api) {
        //Start 10 Minutes Event Loop
        Thread t = new Thread(() -> {
            while(true) {
                every10Minutes(api);
                try {
                    Thread.sleep(1000 * 60 * 10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
        t.setName("clock_10min");
        t.start();

        try {
            while (true) {
                Duration duration = Duration.between(Instant.now(), Tools.setInstantToNextHour(Instant.now()));
                Thread.sleep(duration.getSeconds() * 1000 + duration.getNano() / 1000000);
                onHourStart(api);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void onHourStart(DiscordApi api) {
        Calendar calendar = Calendar.getInstance();
        if (calendar.get(Calendar.HOUR_OF_DAY) == 0) {
            onDayStart(api);
        } else if (calendar.get(Calendar.HOUR_OF_DAY) == 1) {
            Thread t = new Thread(() -> {
                //Backup Database
                try {
                    DBMain.backupAll();
                } catch (IOException | SQLException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            });
            t.setPriority(1);
            t.setName("database_backup");
            t.start();
        }
    }

    private static void onDayStart(DiscordApi api) {
        FisheryCache.getInstance().reset(); //Reset Fishing Limit
        SellCommand.resetCoinsPerFish(); //Reset Fishery Exchange Rate
        trafficWarned = false; //Reset Traffic Warning
        SubredditContainer.getInstance().reset(); //Resets Subreddit Cache
        RunningCommandManager.getInstance().clear(); //Resets Running Commands

        //Survey Results
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.MONDAY || day == Calendar.THURSDAY) {
            try {
                SurveyVotesCollector collector = new SurveyVotesCollector(DBSurvey.getCurrentSurvey().getId());
                List<SurveyServer> serverList = DBSurvey.getUsersWithRightChoiceForCurrentSurvey(api);
                DBSurvey.nextSurvey();
                collector.setResults(DBSurvey.getResults());

                for (SurveyServer surveyServer : serverList) {
                    Locale locale = DBServer.getServerLocale(surveyServer.getServer());
                    for (SurveyUser surveyUser : surveyServer.getUserList()) {
                        try {
                            User user = surveyUser.getUser();

                            long gains = 0;
                            if (surveyUser.isRightChoice()) {
                                gains = DBUser.getFishingProfile(surveyServer.getServer(), user).getEffect(FishingCategoryInterface.PER_SURVEY);
                                DBUser.addFishingValues(locale, surveyServer.getServer(), user, 0, gains);
                            }

                            collector.add(user, surveyServer.getServer(), gains, locale);
                        } catch (IOException | SQLException e) {
                            e.printStackTrace();
                        }
                    }
                }

                for (SurveyCollectorSlot slot : collector.getSlots()) {
                    try {
                        Locale locale = slot.getLocale();
                        String[] surveyData = SurveyCommand.getSurveyData(collector.getSurveyId(), locale);
                        SurveyResults surveyResults = collector.getSurveyResults();
                        int answerWonId = surveyResults.getWinner();
                        String answerWon;
                        final String TROPHY = "\uD83C\uDFC6";

                        if (answerWonId < 2)
                            answerWon = TROPHY + " " + surveyData[answerWonId + 1].toUpperCase() + " " + TROPHY;
                        else
                            answerWon = TextManager.getString(locale, TextManager.COMMANDS, "survey_results_draw").toUpperCase();

                        int percentage = 50;
                        if (answerWonId < 2) percentage = (int) Math.round(surveyResults.getUserVoteRelative(answerWonId) * 100);

                        EmbedBuilder eb = EmbedFactory.getEmbed()
                                .setTitle(TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_title"))
                                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_template",
                                        surveyData[0], surveyData[1], surveyData[2], answerWon, String.valueOf(percentage)));

                        StringBuilder[] stringBuilder = {new StringBuilder(), new StringBuilder()};

                        for (Server server : slot.getServers()) {
                            long gains = slot.getServerGains(server);

                            int type = 1;
                            if (gains > 0) type = 0;

                            stringBuilder[type].append("• ").append(server.getName());

                            if (gains > 0) {
                                stringBuilder[type].append(" (**+").append(Settings.COINS).append(" ").append(Tools.numToString(locale, gains)).append("**)");
                            }

                            stringBuilder[type].append("\n");
                        }

                        for(int i = 0; i < stringBuilder.length; i++) {
                            String str = stringBuilder[i].toString();

                            if (str.length() > 0) {
                                eb.addField(
                                        TextManager.getString(locale, TextManager.COMMANDS, "survey_results_message_wonlost", i),
                                        str,
                                        false
                                );
                            }
                        }

                        Thread t = new Thread(() -> {
                            try {
                                slot.getUser().sendMessage(eb).get();
                            } catch (InterruptedException | ExecutionException e) {
                                //Ignore
                            }
                        });
                        t.setName("survey_notif_sender");
                        t.setPriority(5);
                        t.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        DonationServer.checkExpiredDonations(api); //Check Expired Donations

        //Send Bot Stats
        try {
            DBBot.addStatCommandUsages();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            DBBot.addStatServers(api.getServers().size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            DBBot.addStatUpvotes();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void every10Minutes(DiscordApi api) {
        //Cleans Cooldown List
        Cooldown.getInstance().clean();

        //Analyzes Traffic
        double trafficGB = SIGNALTRANSMITTER.getInstance().getTrafficGB();
        Console.getInstance().setTraffic(trafficGB);

        if (trafficGB >= 20 && (!trafficWarned || trafficGB >= 40)) {
            try {
                api.getOwner().get().sendMessage("Traffic Warning! " + trafficGB + " GB!");
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            trafficWarned = true;
        }
        if (trafficGB >= 40) {
            System.out.println("Traffic Exceeded!");
            System.exit(1);
        }

        //Checks Database Connection
        if (!DBMain.getInstance().checkConnection()) DBMain.getInstance().connect();

        //Updates Activity
        Connector.updateActivity(api);

        //Updates Discord Bots Server Count
        DiscordbotsAPI.getInstance().updateServerCount(api);
    }

    public static boolean instantHasHour(Instant instant, int hour) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
        return calendar.get(Calendar.HOUR_OF_DAY) == hour;
    }

    public static boolean instantHasWeekday(Instant instant, int weekday) {
        Calendar calendar = GregorianCalendar.from(ZonedDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
        return calendar.get(Calendar.DAY_OF_WEEK) == weekday;
    }
}
