package commands.runnables.fisherycategory;

import java.util.Locale;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "acc",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        emoji = "\uD83D\uDE4B",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "profile", "profil", "account", "balance", "bal", "a" }
)
public class AccountCommand extends FisheryMemberAccountInterface {

    public AccountCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(GuildMessageReceivedEvent event, Member member, boolean memberIsAuthor, String args) throws Throwable {
        return DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getMemberData(member.getIdLong())
                .getAccountEmbed();
    }

}
