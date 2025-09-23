package commands.runnables.fisherycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryInterface;
import commands.runnables.NavigationAbstract;
import core.EmbedFactory;
import core.ListGen;
import core.utils.StringUtil;
import mysql.redis.fisheryusers.FisheryMemberBankDeposit;
import mysql.redis.fisheryusers.FisheryMemberData;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "bank",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "🏦",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        releaseDate = { 2025, 10, 7 }
)
public class BankCommand extends NavigationAbstract implements FisheryInterface {

    public BankCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onFisheryAccess(CommandEvent event, String args) throws Throwable {
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonMain(ButtonInteractionEvent event, int i) {
        switch (i) {
            case -1 -> {
                deregisterListenersWithComponentMessage();
                return false;
            }
            case 0 -> {
                return true; //TODO
            }
            case 1 -> {
                return true; //TODO
            }
        }
        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDrawMain(Member member) throws IOException, ExecutionException, InterruptedException {
        FisheryMemberData fisheryMemberData = FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong());
        String list = new ListGen<FisheryMemberBankDeposit>().getList(fisheryMemberData.getBankDeposits(), getLocale(), deposit ->
                getString("deposits_slot", Instant.now().isAfter(deposit.getUntil()), StringUtil.numToString(deposit.getCoins()), TimeFormat.RELATIVE.atInstant(deposit.getUntil()).toString())
        );

        setComponents(getString("options").split("\n"));
        return EmbedFactory.getEmbedDefault(this)
                .setDescription(getString("description"))
                .addField(getString("deposits_title"), list, false);
    }

}
