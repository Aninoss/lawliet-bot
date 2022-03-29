package events.scheduleevents.events;

import java.time.Duration;
import java.time.Instant;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import commands.Command;
import commands.runnables.moderationcategory.MuteCommand;
import constants.ExceptionRunnable;
import core.GlobalThreadPool;
import core.MainLogger;
import core.MemberCacheController;
import core.Program;
import events.scheduleevents.ScheduleEventDaily;
import mysql.modules.guild.DBGuild;
import mysql.modules.servermute.DBServerMute;

@ScheduleEventDaily
public class MuteRefresh implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY && Program.productionMode()) {
            execute();
        }
    }

    public static void execute() {
        GlobalThreadPool.getExecutorService().submit(() -> {
            MainLogger.get().info("Starting mute refresher...");
            AtomicInteger counter = new AtomicInteger(0);
            Random r = new Random();

            DBServerMute.getInstance().retrieveAll().stream()
                    .filter(serverMuteData -> serverMuteData.getGuild().isPresent() &&
                            serverMuteData.isNewMethod() &&
                            serverMuteData.getExpirationTime().orElse(Instant.MAX).isAfter(Instant.now().plus(Duration.ofDays(10)))
                    )
                    .forEach(serverMuteData -> {
                        try {
                            Locale locale = DBGuild.getInstance().retrieve(serverMuteData.getGuildId()).getLocale();
                            MemberCacheController.getInstance().loadMember(serverMuteData.getGuild().get(), serverMuteData.getMemberId()).thenAccept(member -> {
                                if (member.getTimeOutEnd() != null) {
                                    Instant timeOutEnd = member.getTimeOutEnd().toInstant();
                                    Instant expirationMax = Instant.now().plus(Duration.ofDays(27));
                                    Instant expiration = serverMuteData.getExpirationTime().orElse(Instant.MAX);
                                    if (expiration.isAfter(expirationMax)) {
                                        expiration = expirationMax;
                                    }
                                    if (expiration.isAfter(timeOutEnd)) {
                                        try {
                                            Thread.sleep(2000 + r.nextInt(3000));
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        counter.incrementAndGet();
                                        member.timeoutUntil(expiration)
                                                .reason(Command.getCommandLanguage(MuteCommand.class, locale).getTitle())
                                                .complete();
                                    }
                                }
                            });
                        } catch (Throwable e) {
                            MainLogger.get().error("Mute refresher exception for guild: {}; member: {}",
                                    serverMuteData.getGuildId(), serverMuteData.getMemberId(), e
                            );
                        }
                    });

            MainLogger.get().info("Mute refresher completed with {} actions", counter.get());
        });
    }

}
