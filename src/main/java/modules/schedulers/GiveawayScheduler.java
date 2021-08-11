package modules.schedulers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.utilitycategory.GiveawayCommand;
import constants.Category;
import constants.Emojis;
import core.*;
import core.schedule.MainScheduler;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class GiveawayScheduler extends Startable {

    private static final GiveawayScheduler ourInstance = new GiveawayScheduler();

    public static GiveawayScheduler getInstance() {
        return ourInstance;
    }

    private GiveawayScheduler() {
    }

    @Override
    protected void run() {
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
                            processGiveawayUsers(giveawayData, giveawayData.getWinners(), false);
                        } catch (Throwable e) {
                            MainLogger.get().error("Error in giveaway", e);
                        }
                    });
        }
    }

    public CompletableFuture<Boolean> processGiveawayUsers(GiveawayData giveawayData, int numberOfWinners, boolean reroll) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        giveawayData.retrieveMessage()
                .exceptionally(e -> {
                    future.complete(false);
                    giveawayData.stop();
                    return null;
                })
                .thenAccept(message -> {
                    future.complete(true);
                    for (MessageReaction reaction : message.getReactions()) {
                        if (EmojiUtil.reactionEmoteEqualsEmoji(reaction.getReactionEmote(), giveawayData.getEmoji())) {
                            reaction.retrieveUsers().queue(users ->
                                    processGiveaway(giveawayData, message, new ArrayList<>(users), numberOfWinners, reroll)
                            );
                            break;
                        }
                    }
                });
        return future;
    }

    private void processGiveaway(GiveawayData giveawayData, Message message, ArrayList<User> users, int numberOfWinners,
                                boolean reroll
    ) {
        TextChannel channel = message.getTextChannel();
        MemberCacheController.getInstance().loadMembersWithUsers(channel.getGuild(), users).thenAccept(members -> {
            users.removeIf(user -> user.isBot() || !channel.getGuild().isMember(user) || message.getMentionedMembers().stream().anyMatch(m -> m.getIdLong() == user.getIdLong()));
            Collections.shuffle(users);
            List<User> winners = users.subList(0, Math.min(users.size(), numberOfWinners));
            Locale locale = giveawayData.getGuildData().getLocale();

            StringBuilder mentions = new StringBuilder();
            for (User user : winners) {
                mentions.append(user.getAsMention()).append(" ");
            }

            CommandProperties commandProps = Command.getCommandProperties(GiveawayCommand.class);
            EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                    .setTitle(TextManager.getString(locale, Category.UTILITY, "giveaway_results_title", reroll, commandProps.emoji(), giveawayData.getTitle()))
                    .setDescription(TextManager.getString(locale, "utility", "giveaway_results", winners.size() != 1));
            giveawayData.getImageUrl().ifPresent(eb::setImage);
            if (winners.size() > 0) {
                eb.addField(
                        Emojis.ZERO_WIDTH_SPACE,
                        new ListGen<User>().getList(winners, ListGen.SLOT_TYPE_BULLET, user -> "**" + StringUtil.escapeMarkdown(user.getAsTag()) + "**"),
                        false
                );
            } else {
                eb.setDescription(TextManager.getString(locale, "utility", "giveaway_results_empty"));
            }
            giveawayData.stop();

            if (PermissionCheckRuntime.getInstance().botHasPermission(locale, GiveawayCommand.class, channel, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS)) {
                if (!reroll) {
                    message.editMessageEmbeds(eb.build())
                            .content(winners.size() > 0 ? mentions.toString() : null)
                            .queue();

                    if (winners.size() > 0) {
                        channel.sendMessage(mentions.toString())
                                .flatMap(Message::delete)
                                .queue();
                    }
                } else {
                    channel.sendMessageEmbeds(eb.build())
                            .content(winners.size() > 0 ? mentions.toString() : null)
                            .queue();
                }
            }
        });
    }

}
