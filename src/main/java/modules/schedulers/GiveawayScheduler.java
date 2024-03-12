package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.listeners.CommandProperties;
import commands.runnables.configurationcategory.GiveawayCommand;
import constants.Emojis;
import core.*;
import core.schedule.MainScheduler;
import core.utils.StringUtil;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.guild.GuildEntity;
import mysql.modules.giveaway.DBGiveaway;
import mysql.modules.giveaway.GiveawayData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.channel.middleman.StandardGuildMessageChannel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class GiveawayScheduler {

    public static void start() {
        try {
            DBGiveaway.getInstance().retrieveAll().stream()
                    .filter(GiveawayData::isActive)
                    .forEach(GiveawayScheduler::loadGiveawayBean);
        } catch (Throwable e) {
            MainLogger.get().error("Could not start giveaway", e);
        }
    }

    public static void loadGiveawayBean(GiveawayData slot) {
        loadGiveawayBean(slot.getGuildId(), slot.getMessageId(), slot.getEnd());
    }

    public static void loadGiveawayBean(long guildId, long messageId, Instant due) {
        MainScheduler.schedule(due, () -> {
            CustomObservableMap<Long, GiveawayData> map = DBGiveaway.getInstance().retrieve(guildId);
            if (map.containsKey(messageId) && ShardManager.guildIsManaged(guildId)) {
                onGiveawayDue(map.get(messageId));
            }
        });
    }

    private static void onGiveawayDue(GiveawayData giveawayData) {
        if (giveawayData.isActive()) {
            ShardManager.getLocalGuildById(giveawayData.getGuildId())
                    .map(guild -> guild.getChannelById(StandardGuildMessageChannel.class, giveawayData.getStandardGuildMessageChannelId()))
                    .ifPresent(channel -> {
                        try {
                            processGiveawayUsers(giveawayData, giveawayData.getWinners(), false);
                        } catch (Throwable e) {
                            MainLogger.get().error("Error in giveaway", e);
                        }
                    });
        }
    }

    public static CompletableFuture<Boolean> processGiveawayUsers(GiveawayData giveawayData, int numberOfWinners, boolean reroll) {
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
                        if (reaction.getEmoji().getFormatted().equals(giveawayData.getEmoji())) {
                            reaction.retrieveUsers().queue(users -> {
                                        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(giveawayData.getGuildId(), GiveawayScheduler.class)) {
                                            processGiveaway(giveawayData, guildEntity, message, new ArrayList<>(users), numberOfWinners, reroll);
                                        }
                                    }
                            );
                            break;
                        }
                    }
                });
        return future;
    }

    private static void processGiveaway(GiveawayData giveawayData, GuildEntity guildEntity, Message message, ArrayList<User> users, int numberOfWinners,
                                        boolean reroll
    ) {
        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel();
        MemberCacheController.getInstance().loadMembersWithUsers(channel.getGuild(), users).join();

        users.removeIf(user -> user.isBot() || !channel.getGuild().isMember(user) || message.getMentions().getMembers().stream().anyMatch(m -> m.getIdLong() == user.getIdLong()));
        Collections.shuffle(users);
        List<User> winners = users.subList(0, Math.min(users.size(), numberOfWinners));
        Locale locale = guildEntity.getLocale();

        StringBuilder mentions = new StringBuilder();
        for (User user : winners) {
            mentions.append(user.getAsMention()).append(" ");
        }

        CommandProperties commandProps = Command.getCommandProperties(GiveawayCommand.class);
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(locale, Category.CONFIGURATION, "giveaway_results_title", reroll, commandProps.emoji(), giveawayData.getTitle()))
                .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "giveaway_results", winners.size() != 1))
                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));
        giveawayData.getImageUrl().ifPresent(eb::setImage);
        if (!winners.isEmpty()) {
            eb.addField(
                    Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                    new ListGen<User>().getList(winners, ListGen.SLOT_TYPE_BULLET, user -> "**" + StringUtil.escapeMarkdown(user.getName()) + "**"),
                    false
            );
        } else {
            eb.setDescription(TextManager.getString(locale, Category.CONFIGURATION, "giveaway_results_empty"));
        }
        giveawayData.stop();

        if (PermissionCheckRuntime.botHasPermission(locale, GiveawayCommand.class, channel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS)) {
            if (!reroll) {
                message.editMessageEmbeds(eb.build())
                        .setContent(!winners.isEmpty() ? mentions.toString() : null)
                        .queue();

                if (!winners.isEmpty()) {
                    channel.sendMessage(mentions.toString())
                            .flatMap(Message::delete)
                            .queue();
                }
            } else {
                channel.sendMessageEmbeds(eb.build())
                        .setContent(!winners.isEmpty() ? mentions.toString() : null)
                        .queue();
            }
        }
    }

}
