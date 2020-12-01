package modules.schedulers;

import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.utilitycategory.GiveawayCommand;
import constants.Emojis;
import constants.Permission;
import core.*;
import core.schedule.MainScheduler;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GiveawayScheduler {

    private final static Logger LOGGER = LoggerFactory.getLogger(GiveawayScheduler.class);
    private static final GiveawayScheduler ourInstance = new GiveawayScheduler();

    public static GiveawayScheduler getInstance() {
        return ourInstance;
    }

    private GiveawayScheduler() {
    }

    private boolean started = false;

    public void start() {
        if (started) return;
        started = true;

        try {
            DBGiveaway.getInstance().getBean().values().stream()
                    .filter(GiveawayBean::isActive)
                    .forEach(this::loadGiveawayBean);
        } catch (Exception e) {
            LOGGER.error("Could not start giveaway", e);
        }
    }

    public void loadGiveawayBean(GiveawayBean giveawayBean) {
        MainScheduler.getInstance().schedule(giveawayBean.getEnd(), "giveaway", () -> {
            onGiveawayDue(giveawayBean);
        });
    }

    private void onGiveawayDue(GiveawayBean giveawayBean) {
        if (giveawayBean.isActive()) {
            DiscordApiCollection.getInstance().getServerById(giveawayBean.getServerId())
                    .flatMap(server -> server.getTextChannelById(giveawayBean.getChannelId()))
                    .ifPresent(channel -> {
                        try {
                            processGiveawayUsers(channel, DBServer.getInstance().getBean(channel.getServer().getId()), giveawayBean);
                        } catch (Throwable e) {
                            LOGGER.error("Error in giveaway", e);
                        }
                    });
        }
    }

    private void processGiveawayUsers(ServerTextChannel channel, ServerBean serverBean, GiveawayBean giveawayBean) {
        DiscordApiCollection.getInstance().getMessageById(channel, giveawayBean.getMessageId()).ifPresent(message -> {
            for (Reaction reaction : message.getReactions()) {
                if (reaction.getEmoji().getMentionTag().equals(giveawayBean.getEmoji())) {
                    reaction.getUsers().thenAccept(users -> processGiveaway(channel, serverBean, giveawayBean, message, new ArrayList<>(users)));
                    break;
                }
            }
        });
    }

    private void processGiveaway(ServerTextChannel channel, ServerBean serverBean, GiveawayBean giveawayBean, Message message, ArrayList<User> users) {
        users.removeIf(user -> user.isBot() || channel.getServer().getMemberById(user.getId()).isEmpty());
        Collections.shuffle(users);
        List<User> winners = users.subList(0, Math.min(users.size(), giveawayBean.getWinners()));

        StringBuilder mentions = new StringBuilder();
        for (User user : winners) {
            mentions.append(user.getMentionTag()).append(" ");
        }

        CommandProperties commandProps = Command.getClassProperties(GiveawayCommand.class);
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProps.emoji() + " " + giveawayBean.getTitle())
                .setDescription(TextManager.getString(serverBean.getLocale(), "utility", "giveaway_results", winners.size() != 1));
        giveawayBean.getImageUrl().ifPresent(eb::setImage);
        if (winners.size() > 0)
            eb.addField(Emojis.EMPTY_EMOJI, new ListGen<User>().getList(winners, ListGen.SLOT_TYPE_BULLET, user -> "**" + user.getDiscriminatedName() + "**"));
        else
            eb.setDescription(TextManager.getString(serverBean.getLocale(), "utility", "giveaway_results_empty"));

        giveawayBean.stop();
        message.edit(mentions.toString(), eb).exceptionally(ExceptionLogger.get());
        if (PermissionCheckRuntime.getInstance().botHasPermission(serverBean.getLocale(), GiveawayCommand.class, channel, Permission.READ_MESSAGE_HISTORY | Permission.SEND_MESSAGES | Permission.EMBED_LINKS))
            channel.sendMessage(mentions.toString()).thenAccept(m -> m.delete().exceptionally(ExceptionLogger.get()));
    }

}
