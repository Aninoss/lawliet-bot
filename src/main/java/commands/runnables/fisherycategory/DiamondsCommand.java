package commands.runnables.fisherycategory;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.listeners.OnButtonListener;
import constants.AssetIds;
import constants.LogStatus;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import core.utils.EmbedUtil;
import mysql.redis.fisheryusers.FisheryUserManager;
import mysql.redis.fisheryusers.FisheryMemberData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;

@CommandProperties(
        trigger = "diamonds",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "💎",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "diamanten", "gems", "dia" },
        exclusiveGuilds = { AssetIds.ANICORD_SERVER_ID, AssetIds.BETA_SERVER_ID }
)
public class DiamondsCommand extends Command implements OnButtonListener {

    private EmbedBuilder accountChangeEmbed = null;

    public DiamondsCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerButtonListener(event.getMember())
                .exceptionally(ExceptionLogger.get());
        return true;
    }

    @Override
    public synchronized boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(event.getGuild().getIdLong())
                .getMemberData(event.getMember().getIdLong());
        if (fisheryMemberData.getDiamonds() >= 3) {
            fisheryMemberData.removeThreeDiamonds();
            long coins = fisheryMemberData.getCoins();
            accountChangeEmbed = fisheryMemberData.changeValuesEmbed(event.getMember(), 0L, coins, getGuildEntity());
            setLog(LogStatus.SUCCESS, "Du hast deine Coins verdoppelt!");
            deregisterListeners();
        } else {
            EmbedBuilder errEmbed = EmbedFactory.getEmbedError()
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "rejected"))
                    .setDescription("Du hast nicht genug Diamanten!");
            getInteractionResponse().replyEmbeds(List.of(errEmbed.build()), true)
                    .queue();
        }
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member) throws Throwable {
        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
        if (accountChangeEmbed != null) {
            setAdditionalEmbeds(accountChangeEmbed.build());
        } else {
            setComponents("3 Diamanten einlösen");
        }
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription("💎 Diamanten: " + fisheryMemberData.getDiamonds() + " / 3");
        EmbedUtil.setMemberAuthor(eb, member);
        return eb;
    }

}
