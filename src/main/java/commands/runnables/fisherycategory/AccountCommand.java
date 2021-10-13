package commands.runnables.fisherycategory;

import java.util.Locale;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.FisheryMemberAccountInterface;
import core.EmbedFactory;
import core.TextManager;
import mysql.modules.fisheryusers.DBFishery;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;

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
        if (!member.getUser().isBot()) {
            return DBFishery.getInstance().retrieve(member.getGuild().getIdLong()).getMemberData(member.getIdLong())
                    .getAccountEmbed(member);
        } else {
            return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), TextManager.GENERAL, "no_bots"));
        }
    }

}
