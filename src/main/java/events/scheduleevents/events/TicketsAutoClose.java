package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.MainLogger;
import core.ShardManager;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import events.scheduleevents.ScheduleEventHourly;
import modules.Ticket;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.ticket.DBTicket;
import mysql.modules.ticket.TicketChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ScheduleEventHourly
public class TicketsAutoClose implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        MainLogger.get().info("Starting ticket auto closer...");
        AtomicInteger counter = new AtomicInteger(0);

        DBTicket.getInstance().retrieveAllGuildIdsWithAutoClose().stream()
                .filter(guildId -> ShardManager.getLocalGuildById(guildId).isPresent())
                .map(guildId -> DBTicket.getInstance().retrieve(guildId))
                .filter(ticketData -> ticketData.getAutoCloseHoursEffectively() != null)
                .forEach(ticketData -> {
                    for (TicketChannel ticketChannel : new ArrayList<>(ticketData.getTicketChannels().values())) {
                        TextChannel textChannel = ticketChannel.getTextChannel().orElse(null);
                        if (textChannel == null) {
                            continue;
                        }

                        List<Message> messages = textChannel.getHistory().retrievePast(25).complete().stream()
                                .filter(m -> !m.isWebhookMessage() && !m.getAuthor().isBot())
                                .collect(Collectors.toList());

                        if (!messages.isEmpty() &&
                                messages.get(0).getAuthor().getIdLong() != ticketChannel.getMemberId() &&
                                messages.get(0).getTimeCreated().toInstant().plus(Duration.ofHours(ticketData.getAutoCloseHours())).isBefore(Instant.now())
                        ) {
                            FeatureLogger.inc(PremiumFeature.TICKETS, ticketData.getGuildId());
                            try (GuildEntity guildEntity = HibernateManager.findGuildEntity(ticketData.getGuildId())) {
                                Ticket.closeTicket(ticketData, guildEntity, textChannel, ticketChannel);
                            }
                            counter.incrementAndGet();
                        }
                    }
                });

        MainLogger.get().info("Ticket auto closer completed with {} actions", counter.get());
    }

}
