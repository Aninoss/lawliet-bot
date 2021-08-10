package websockets.syncserver.events;

import java.text.MessageFormat;
import constants.AssetIds;
import core.*;
import core.utils.StringUtil;
import modules.Fishery;
import mysql.modules.fisheryusers.DBFishery;
import mysql.modules.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import websockets.syncserver.SyncServerEvent;

@SyncServerEvent(event = "TOPGG_ANICORD")
public class OnTopGGAnicord extends OnTopGG {

    @Override
    protected void processUpvote(long userId, boolean isWeekend) {
        MainLogger.get().info("OnTopGGAnicord | 0"); //TODO
        Guild guild = ShardManager.getInstance().getLocalGuildById(AssetIds.ANICORD_SERVER_ID).get();
        MainLogger.get().info("OnTopGGAnicord | 1"); //TODO
        MemberCacheController.getInstance().loadMember(guild, userId).thenAccept(member -> {
            MainLogger.get().info("OnTopGGAnicord | 2"); //TODO
            if (member != null) {
                MainLogger.get().info("OnTopGGAnicord | 3"); //TODO
                TextChannel bumpChannel = guild.getTextChannelById(713849992611102781L);

                FisheryMemberData userBean = DBFishery.getInstance().retrieve(guild.getIdLong()).getMemberData(userId);
                MainLogger.get().info("OnTopGGAnicord | 4"); //TODO
                long add = Fishery.getClaimValue(userBean);
                MainLogger.get().info("OnTopGGAnicord | 5"); //TODO

                String desc = MessageFormat.format("✅ | {0} hat auf [top.gg]({3}) für **{1}** geupvotet und dafür **🐟 {2}** (25% der Daily-Fische) erhalten!", member.getAsMention(), guild.getName(), StringUtil.numToString(add), String.format("https://top.gg/servers/%d/vote", AssetIds.ANICORD_SERVER_ID));
                bumpChannel.sendMessageEmbeds(
                        EmbedFactory.getEmbedDefault().setDescription(desc).build(),
                        userBean.changeValuesEmbed(member, add, 0).build()
                ).queue();
                MainLogger.get().info("OnTopGGAnicord | 6"); //TODO
            }
        }).exceptionally(ExceptionLogger.get());
        MainLogger.get().info("OnTopGGAnicord | 7"); //TODO
    }

}
