package events.sync.events;

import commands.Category;
import commands.Command;
import commands.runnables.fisherysettingscategory.VoteRewardsCommand;
import core.*;
import core.cache.ServerPatreonBoostCache;
import core.featurelogger.FeatureLogger;
import core.featurelogger.PremiumFeature;
import core.utils.StringUtil;
import events.sync.SyncServerEvent;
import events.sync.SyncServerFunction;
import modules.fishery.FisheryGear;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.FisheryEntity;
import mysql.hibernate.entity.GuildEntity;
import mysql.modules.bannedusers.DBBannedUsers;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.json.JSONObject;

@SyncServerEvent(event = "TOPGG_VOTE_REWARDS")
public class OnTopGGVoteRewards implements SyncServerFunction {

    @Override
    public JSONObject apply(JSONObject jsonObject) {
        long guildId = jsonObject.getLong("guild");
        long userId = jsonObject.getLong("user");
        String type = jsonObject.getString("type");
        String auth = jsonObject.getString("auth");
        Guild guild = ShardManager.getLocalGuildById(guildId).orElse(null);

        if (guild == null ||
                DBBannedUsers.getInstance().retrieve().getSlotsMap().containsKey(userId) ||
                !ServerPatreonBoostCache.get(guildId)) {
            return generateJsonObject(false);
        }

        try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guildId)) {
            FisheryEntity fisheryEntity = guildEntity.getFishery();
            if (!fisheryEntity.getVoteRewardsActive() ||
                    !auth.equals(fisheryEntity.getVoteRewardsAuthorization())) {
                return generateJsonObject(false);
            }
            FeatureLogger.inc(PremiumFeature.FISHERY, guildId);

            GuildMessageChannel voteRewardsChannel = fisheryEntity.getVoteRewardsChannel().get().orElse(null);
            if (!type.equals("upvote")) {
                if (voteRewardsChannel != null) {
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(Command.getCommandLanguage(VoteRewardsCommand.class, guildEntity.getLocale()).getTitle())
                            .setDescription(TextManager.getString(guildEntity.getLocale(), Category.FISHERY_SETTINGS, "voterewards_embed_test"));
                    voteRewardsChannel.sendMessageEmbeds(eb.build()).queue();
                }
                MainLogger.get().info("UPVOTE TEST {} | {}", guildId, userId);
                return generateJsonObject(true);
            }

            MemberCacheController.getInstance().loadMember(guild, userId).thenAccept(member -> {
                if (member == null) {
                    return;
                }

                FisheryMemberData fisheryMemberData = DBFishery.getInstance().retrieve(guildId).getMemberData(userId);
                long add = Math.round(fisheryMemberData.getMemberGear(FisheryGear.DAILY).getEffect() * fisheryEntity.getVoteRewardsDailyPortionInPercent() / 100.0);

                if (voteRewardsChannel != null && PermissionCheckRuntime.botHasPermission(guildEntity.getLocale(), VoteRewardsCommand.class, voteRewardsChannel, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_EXT_EMOJI)) {
                    String desc = TextManager.getString(guildEntity.getLocale(), Category.FISHERY_SETTINGS, "voterewards_embed_upvote",
                            StringUtil.escapeMarkdown(member.getEffectiveName()),
                            StringUtil.escapeMarkdown(guild.getName()),
                            "https://top.gg/servers/" + guildId + "/vote",
                            StringUtil.numToString(fisheryEntity.getVoteRewardsDailyPortionInPercent())
                    );
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(Command.getCommandLanguage(VoteRewardsCommand.class, guildEntity.getLocale()).getTitle())
                            .setDescription(desc);
                    voteRewardsChannel.sendMessageEmbeds(
                            eb.build(),
                            fisheryMemberData.changeValuesEmbed(member, add, 0, guildEntity).build()
                    ).queue();
                } else {
                    fisheryMemberData.changeValues(add, 0);
                }
            }).exceptionally(ExceptionLogger.get());

            MainLogger.get().info("UPVOTE {} | {}", guildId, userId);
            return generateJsonObject(true);
        }
    }

    private JSONObject generateJsonObject(boolean success) {
        return new JSONObject()
                .put("success", success);
    }

}
