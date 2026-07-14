package commands.runnables.fisherycategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import mysql.redis.fisheryusers.FisheryUserManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

import java.util.Locale;

@CommandProperties(
        trigger = "acc",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDE4B",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        requiresFullMemberCache = true,
        aliases = { "profile", "profil", "account", "balance", "bal", "a" }
)
public class AccountCommand extends FisheryMemberAccountInterface {

    public AccountCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        return FisheryUserManager.getGuildData(member.getGuild().getIdLong()).getMemberData(member.getIdLong())
                .getAccountEmbed(member, getGuildEntity());
    }

}
