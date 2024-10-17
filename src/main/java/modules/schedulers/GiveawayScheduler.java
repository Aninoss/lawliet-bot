package modules.schedulers;

import commands.Category;
import commands.Command;
import commands.runnables.configurationcategory.GiveawayCommand;
import constants.Emojis;
import core.*;
import core.atomicassets.AtomicRole;
import core.schedule.MainScheduler;
import core.utils.BotPermissionUtil;
import core.utils.EmojiUtil;
import core.utils.StringUtil;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GiveawayEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.time.Instant;
import java.util.*;

public class GiveawayScheduler {

    public static void start() {
        try (EntityManagerWrapper entityManager = HibernateManager.createEntityManager(GiveawayScheduler.class)) {
            entityManager.findAllForResponsibleIds(GiveawayEntity.class, "guildId")
                    .forEach(giveaway -> {
                        if (giveaway.getActive()) {
                            loadGiveaway(giveaway);
                        }
                    });
        } catch (Throwable e) {
            MainLogger.get().error("Could not start giveaways", e);
        }
    }

    public static void loadGiveaway(GiveawayEntity giveaway) {
        loadGiveaway(giveaway.getGuildId(), giveaway.getMessageId(), giveaway.getEnd());
    }

    public static void loadGiveaway(long guildId, long messageId, Instant due) {
        MainScheduler.schedule(due, () -> {
            try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId, GiveawayScheduler.class)) {
                Map<Long, GiveawayEntity> giveaways = guildEntity.getGiveaways();
                if (giveaways.containsKey(messageId) && ShardManager.guildIsManaged(guildId)) {
                    onGiveawayDue(giveaways.get(messageId), guildEntity);
                }
            }
        });
    }

    private static void onGiveawayDue(GiveawayEntity giveaway, GuildEntity guildEntity) {
       if (!giveaway.getActive()) {
            return;
        }

        guildEntity.beginTransaction();
        giveaway.setActive(false);
        guildEntity.commitTransaction();

        ShardManager.getLocalGuildById(giveaway.getGuildId())
                .map(guild -> guild.getChannelById(GuildMessageChannel.class, giveaway.getChannelId()))
                .ifPresent(channel -> {
                    try {
                        processGiveawayUsers(giveaway, guildEntity.getLocale(), giveaway.getWinners(), false);
                    } catch (Throwable e) {
                        MainLogger.get().error("Error in giveaway", e);
                    }
                });
    }

    public static boolean processGiveawayUsers(GiveawayEntity giveaway, Locale locale, int winners, boolean reroll) {
        Message message;
        try {
            message = ShardManager.getLocalGuildById(giveaway.getGuildId())
                    .map(guild -> guild.getChannelById(GuildMessageChannel.class, giveaway.getChannelId()))
                    .map(channel -> {
                        if (BotPermissionUtil.canReadHistory(channel)) {
                            return channel.retrieveMessageById(giveaway.getMessageId()).complete();
                        } else {
                            return null;
                        }
                    }).orElse(null);
        } catch (Throwable e) {
            //ignore
            return false;
        }
        if (message == null) {
            return false;
        }

        for (MessageReaction reaction : message.getReactions()) {
            if (EmojiUtil.equals(reaction.getEmoji(), giveaway.getEmoji())) {
                List<User> users = reaction.retrieveUsers().complete();
                List<Member> members = MemberCacheController.getInstance().loadMembersWithUsers(message.getGuild(), users).join();
                processGiveaway(giveaway, locale, message, new ArrayList<>(members), winners, reroll);
                return true;
            }
        }

        return false;
    }

    private static void processGiveaway(GiveawayEntity giveaway, Locale locale, Message message, ArrayList<Member> members,
                                        int numberOfWinners, boolean reroll
    ) {
        GuildMessageChannel channel = (GuildMessageChannel) message.getChannel();
        members.removeIf(member -> member.getUser().isBot() || message.getMentions().getMembers().stream().anyMatch(m -> m.getIdLong() == member.getIdLong()));
        Collections.shuffle(members);
        List<Member> winners = members.subList(0, Math.min(members.size(), numberOfWinners));

        StringBuilder mentions = new StringBuilder();
        for (Member member : winners) {
            mentions.append(member.getAsMention()).append(" ");
        }

        String title = giveaway.getItem();
        if (reroll) {
            title += " " + TextManager.getString(locale, Category.CONFIGURATION, "giveaway_results_reroll");
        }
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(title)
                .setDescription(TextManager.getString(locale, Category.CONFIGURATION, "giveaway_results", winners.size() != 1))
                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "serverstaff_text"));

        if (giveaway.getImageUrl() != null) {
            eb.setImage(giveaway.getImageUrl());
        }

        if (!winners.isEmpty()) {
            eb.addField(
                    Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                    new ListGen<Member>().getList(winners, ListGen.SLOT_TYPE_BULLET, member -> "**" + StringUtil.escapeMarkdown(member.getUser().getName()) + "**"),
                    false
            );
        } else {
            eb.setDescription(TextManager.getString(locale, Category.CONFIGURATION, "giveaway_results_empty"));
        }

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

        List<Role> roles = AtomicRole.to(giveaway.getPrizeRoles());
        if (!roles.isEmpty() && !winners.isEmpty() && PermissionCheckRuntime.botCanManageRoles(locale, GiveawayCommand.class, roles)) {
            for (Member member : winners) {
                channel.getGuild().modifyMemberRoles(member, roles, Collections.emptySet())
                        .reason(Command.getCommandLanguage(GiveawayCommand.class, locale).getTitle())
                        .queue();
            }
        }
    }

}
