package events.sync.events;

import constants.AssetIds;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MemberCacheController;
import core.ShardManager;
import core.utils.StringUtil;
import events.sync.SyncServerEvent;
import modules.fishery.Fishery;
import mysql.hibernate.HibernateManager;
import mysql.hibernate.entity.GuildEntity;
import mysql.redis.fisheryusers.FisheryUserManager;
import mysql.redis.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.text.MessageFormat;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord extends OnTopGG {

    @Override
    protected void processUpvote(long userId, boolean isWeekend) {
        Guild guild = ShardManager.getLocalGuildById(AssetIds.ANICORD_SERVER_ID).get();
        MemberCacheController.getInstance().loadMember(guild, userId).thenAccept(member -> {
            if (member != null) {
                TextChannel bumpChannel = guild.getTextChannelById(713849992611102781L);

                FisheryMemberData userBean = FisheryUserManager.getGuildData(guild.getIdLong()).getMemberData(userId);
                long add = Fishery.getClaimValue(userBean);

                String desc = MessageFormat.format("✅ | **{0}** hat auf [top.gg]({3}) für **{1}** geupvotet und dafür **🐟 {2}** (25% der Daily-Fische) erhalten!", StringUtil.escapeMarkdown(member.getEffectiveName()), StringUtil.escapeMarkdown(guild.getName()), StringUtil.numToString(add), String.format("https://top.gg/servers/%d/vote", AssetIds.ANICORD_SERVER_ID));
                try (GuildEntity guildEntity = HibernateManager.findGuildEntity(guild.getIdLong())) {
                    bumpChannel.sendMessageEmbeds(
                            EmbedFactory.getEmbedDefault().setDescription(desc).build(),
                            userBean.changeValuesEmbed(member, add, 0, guildEntity).build()
                    ).queue();
                }
            }
        }).exceptionally(ExceptionLogger.get());
    }

}
