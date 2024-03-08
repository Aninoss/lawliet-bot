package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.ShardManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import events.scheduleevents.ScheduleEventFixedRate;
import modules.Ticket;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.TicketChannelEntity;
import mysql.hibernate.entity.guild.TicketsEntity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ScheduleEventFixedRate(rateValue = 1, rateUnit = ChronoUnit.HOURS)
public class TicketsAutoClose implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        MainLogger.get().info("Starting ticket auto closer...");
        AtomicInteger counter = new AtomicInteger(0);

        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(TicketsAutoClose.class)) {
            TicketsEntity.findAllGuildEntitiesWithTicketsAutoClose(entityManager).stream()
                    .filter(guildEntity -> ShardManager.getLocalGuildById(guildEntity.getGuildId()).isPresent() &&
                            guildEntity.getTickets().getAutoCloseHoursEffectively() != null)
                    .forEach(guildEntity -> {
                        TicketsEntity ticketsEntity = guildEntity.getTickets();

                        for (TicketChannelEntity ticketChannelEntity : new ArrayList<>(ticketsEntity.getTicketChannels().values())) {
                            TextChannel textChannel = ShardManager.getLocalGuildById(guildEntity.getGuildId()).get().getTextChannelById(ticketChannelEntity.getChannelId());
                            if (textChannel == null) {
                                continue;
                            }

                        List<Message> messages = textChannel.getHistory().retrievePast(25).complete().stream()
                                .filter(m -> !m.isWebhookMessage() && !m.getAuthor().isBot())
                                .collect(Collectors.toList());

                            if (!messages.isEmpty() &&
                                    messages.get(0).getAuthor().getIdLong() != ticketChannelEntity.getMemberId() &&
                                    messages.get(0).getTimeCreated().toInstant().plus(Duration.ofHours(ticketsEntity.getAutoCloseHoursEffectively())).isBefore(Instant.now())
                            ) {
                                FeatureLogger.inc(PremiumFeature.TICKETS_AUTO_CLOSE, guildEntity.getGuildId());
                                Ticket.closeTicket(guildEntity, ticketChannelEntity, textChannel);
                                counter.incrementAndGet();
                            }
                        }
                    });
        }

        MainLogger.get().info("Ticket auto closer completed with {} actions", counter.get());
    }

}
