package events.scheduleevents.events;

import commands.Command;
import commands.runnables.moderationcategory.MuteCommand;
import constants.ExceptionRunnable;
import core.MainLogger;
import core.MemberCacheController;
import core.PermissionCheckRuntime;
import core.Program;
import core.utils.BotPermissionUtil;
import events.scheduleevents.ScheduleEventFixedRate;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.modules.servermute.DBServerMute;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@ScheduleEventFixedRate(rateValue = 7, rateUnit = ChronoUnit.DAYS)
public class MuteRefresh implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        if (Program.productionMode()) {
            execute();
        }
    }

    public static void execute() {
        MainLogger.get().info("Starting mute refresher...");
        AtomicInteger counter = new AtomicInteger(0);
        Random r = new Random();

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(MuteRefresh.class)) {
            DBServerMute.getInstance().retrieveAll().stream()
                    .filter(serverMuteData -> serverMuteData.getGuild().isPresent() &&
                            serverMuteData.isNewMethod() &&
                            serverMuteData.getExpirationTime().orElse(Instant.MAX).isAfter(Instant.now().plus(Duration.ofDays(10)))
                    )
                    .forEach(serverMuteData -> {
                        try {
                            Member member = MemberCacheController.getInstance().loadMember(serverMuteData.getGuild().get(), serverMuteData.getMemberId()).get();
                            if (member == null || member.getTimeOutEnd() == null || !member.getGuild().getSelfMember().canInteract(member)) {
                                return;
                            }

                            Instant timeOutEnd = member.getTimeOutEnd().toInstant();
                            Instant expirationMax = Instant.now().plus(Duration.ofDays(27));
                            Instant expiration = serverMuteData.getExpirationTime().orElse(Instant.MAX);
                            if (expiration.isAfter(expirationMax)) {
                                expiration = expirationMax;
                            }
                            if (expiration.isAfter(timeOutEnd)) {
                                Thread.sleep(2000 + r.nextInt(3000));
                                counter.incrementAndGet();
                                Locale locale = entityManager.findGuildEntity(serverMuteData.getGuildId()).getLocale();
                                if (PermissionCheckRuntime.botHasPermission(locale, MuteCommand.class, member.getGuild(), Permission.MODERATE_MEMBERS) && !BotPermissionUtil.can(member, Permission.ADMINISTRATOR)) {
                                    member.timeoutUntil(expiration)
                                            .reason(Command.getCommandLanguage(MuteCommand.class, locale).getTitle())
                                            .complete();
                                }
                            }
                        } catch (Throwable e) {
                            MainLogger.get().error("Mute refresher exception for guild: {}; member: {}",
                                    serverMuteData.getGuildId(), serverMuteData.getMemberId(), e
                            );
                        } finally {
                            entityManager.clear();
                        }
                    });
        }

        MainLogger.get().info("Mute refresher completed with {} actions", counter.get());
    }

}
