package modules.schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.utilitycategory.GiveawayCommand;
import constants.Emojis;
import core.*;
import core.schedule.MainScheduler;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayData;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class GiveawayScheduler {

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
            DBGiveaway.getInstance().retrieveAll().stream()
                    .filter(GiveawayData::isActive)
                    .forEach(this::loadGiveawayBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start giveaway", e);
        }
    }

    public void loadGiveawayBean(GiveawayData slot) {
        loadGiveawayBean(slot.getGuildId(), slot.getMessageId(), slot.getEnd());
    }

    public void loadGiveawayBean(long guildId, long messageId, Instant due) {
        MainScheduler.getInstance().schedule(due, "giveaway_" + messageId, () -> {
            CustomObservableMap<Long, GiveawayData> map = DBGiveaway.getInstance().retrieve(guildId);
            if (map.containsKey(messageId) && ShardManager.getInstance().guildIsManaged(guildId)) {
                onGiveawayDue(map.get(messageId));
            }
        });
    }

    private void onGiveawayDue(GiveawayData giveawayData) {
        if (giveawayData.isActive()) {
            ShardManager.getInstance().getLocalGuildById(giveawayData.getGuildId())
                    .map(guild -> guild.getTextChannelById(giveawayData.getTextChannelId()))
                    .ifPresent(channel -> {
                        try {
                            processGiveawayUsers(channel, DBGuild.getInstance().retrieve(channel.getGuild().getIdLong()), giveawayData);
                        } catch (Throwable e) {
                            MainLogger.get().error("Error in giveaway", e);
                        }
                    });
        }
    }

    private void processGiveawayUsers(TextChannel channel, GuildData guildBean, GiveawayData giveawayData) {
        giveawayData.retrieveMessage()
                .thenAccept(message -> {
                    for (MessageReaction reaction : message.getReactions()) {
                        if (EmojiUtil.reactionEmoteEqualsEmoji(reaction.getReactionEmote(), giveawayData.getEmoji())) {
                            reaction.retrieveUsers().queue(users ->
                                    processGiveaway(channel, guildBean, giveawayData, message, new ArrayList<>(users))
                            );
                            break;
                        }
                    }
                });
    }

    private void processGiveaway(TextChannel channel, GuildData guildBean, GiveawayData giveawayData, Message message, ArrayList<User> users) {
        users.removeIf(user -> user.isBot() || !channel.getGuild().isMember(user));
        Collections.shuffle(users);
        List<User> winners = users.subList(0, Math.min(users.size(), giveawayData.getWinners()));

        StringBuilder mentions = new StringBuilder();
        for (User user : winners) {
            mentions.append(user.getAsMention()).append(" ");
        }

        CommandProperties commandProps = Command.getCommandProperties(GiveawayCommand.class);
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(commandProps.emoji() + " " + giveawayData.getTitle())
                .setDescription(TextManager.getString(guildBean.getLocale(), "utility", "giveaway_results", winners.size() != 1));
        giveawayData.getImageUrl().ifPresent(eb::setImage);
        if (winners.size() > 0) {
            eb.addField(
                    Emojis.ZERO_WIDTH_SPACE,
                    new ListGen<User>().getList(winners, ListGen.SLOT_TYPE_BULLET, user -> "**" + StringUtil.escapeMarkdown(user.getAsTag()) + "**"),
                    false
            );
        } else {
            eb.setDescription(TextManager.getString(guildBean.getLocale(), "utility", "giveaway_results_empty"));
        }

        giveawayData.stop();
        message.editMessage(eb.build())
                .content(winners.size() > 0 ? mentions.toString() : null)
                .queue();

        if (winners.size() > 0 && PermissionCheckRuntime.getInstance().botHasPermission(guildBean.getLocale(), GiveawayCommand.class, channel, Permission.MESSAGE_WRITE)) {
            channel.sendMessage(mentions.toString()).flatMap(Message::delete).queue();
        }
    }

}
