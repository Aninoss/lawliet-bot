package modules.schedulers;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.utilitycategory.GiveawayCommand;
import constants.Emojis;
import constants.Permission;
import core.*;
import core.utils.TimeUtil;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayBean;
import mysql.modules.server.DBServer;
import mysql.modules.server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Reaction;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.util.logging.ExceptionLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class GiveawayScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayScheduler.class);
    private static final GiveawayScheduler ourInstance = new GiveawayScheduler();

    public static GiveawayScheduler getInstance() {
        return ourInstance;
    }

    private GiveawayScheduler() {
    }

    private final Timer timer = new Timer();
    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBGiveaway.getInstance().loadBean().values().stream()
                    .filter(giveawayBean -> giveawayBean.getEnd().isAfter(Instant.now()))
                    .forEach(this::loadGiveawayBean);
        } catch (Exception e) {
            LOGGER.error("Could not start giveaway", e);
        }
    }

    public void loadGiveawayBean(GiveawayBean giveawayBean) {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                onGiveawayDue(giveawayBean);
            }
        }, TimeUtil.getMilisBetweenInstants(Instant.now(), giveawayBean.getStart().plus(giveawayBean.getDurationMinutes(), ChronoUnit.MINUTES)));
    }

    private void onGiveawayDue(GiveawayBean giveawayBean) {
        if (giveawayBean.isActive()) {
            giveawayBean.stop();

            DiscordApiCollection.getInstance().getServerById(giveawayBean.getServerId())
                    .flatMap(server -> server.getTextChannelById(giveawayBean.getChannelId()))
                    .ifPresent(channel -> {
                        try {
                            processGiveaway(channel, DBServer.getInstance().getBean(channel.getServer().getId()), giveawayBean);
                        } catch (Throwable e) {
                            LOGGER.error("Error in giveaway", e);
                        }
                    });
        }
    }

    private void processGiveaway(ServerTextChannel channel, ServerBean serverBean, GiveawayBean giveawayBean) throws ExecutionException, InterruptedException {
        Optional<Message> messageOpt = DiscordApiCollection.getInstance().getMessageById(channel, giveawayBean.getMessageId());
        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();
            ArrayList<User> users = new ArrayList<>();

            for (Reaction reaction : message.getReactions()) {
                if (reaction.getEmoji().getMentionTag().equals(giveawayBean.getEmoji())) {
                    reaction.getUsers().get().forEach(user -> {
                        if (!user.isBot() && channel.getServer().getMembers().contains(user))
                            users.add(user);
                    });
                }
            }
            Collections.shuffle(users);
            List<User> winners = users.subList(0, Math.min(users.size(), giveawayBean.getWinners()));

            if (winners.size() > 0) {
                StringBuilder mentions = new StringBuilder();
                for (User user : winners) {
                    mentions.append(user.getMentionTag()).append(" ");
                }

                CommandProperties commandProps = Command.getClassProperties(GiveawayCommand.class);
                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(commandProps.emoji() + " " + giveawayBean.getTitle())
                        .setDescription(TextManager.getString(serverBean.getLocale(), "utility", "giveaway_results", winners.size() != 1));
                if (winners.size() > 0)
                    eb.addField(Emojis.EMPTY_EMOJI, new ListGen<User>().getList(winners, ListGen.SLOT_TYPE_BULLET, user -> "**" + user.getDiscriminatedName() + "**"));

                giveawayBean.getImageUrl().ifPresent(eb::setImage);

                message.edit(mentions.toString(), eb).exceptionally(ExceptionLogger.get());

                if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), GiveawayCommand.class, channel, Permission.READ_MESSAGE_HISTORY | Permission.SEND_MESSAGES | Permission.EMBED_LINKS))
                    channel.sendMessage(mentions.toString()).thenAccept(m -> m.delete().exceptionally(ExceptionLogger.get()));
            }
        }
    }

}
