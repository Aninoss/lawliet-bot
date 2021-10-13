package commands.runnables.informationcategory;

import java.util.Locale;
import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import constants.AssetIds;
import core.EmbedFactory;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.utils.TimeFormat;

@CommandProperties(
        trigger = "userinfo",
        emoji = "\uD83D\uDC81",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "userinfos", "userstat", "userstats", "accountinfo", "whois", "memberinfo", "user", "member" }
)
public class MemberInfoCommand extends MemberAccountAbstract {

    public MemberInfoCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected EmbedBuilder processMember(CommandEvent event, Member member, boolean memberIsAuthor, String args) {
        String[] type = getString("type").split("\n");
        int typeN = 0;
        if (!member.getUser().isBot()) {
            typeN = 1;
            if (event.getGuild().getOwnerIdLong() == member.getIdLong()) typeN = 2;
            if (member.getIdLong() == AssetIds.OWNER_USER_ID) typeN = 3;
        }

        String[] argsArray = {
                type[typeN],
                StringUtil.escapeMarkdown(member.getUser().getName()),
                member.getNickname() != null ? member.getNickname() : "-",
                member.getUser().getDiscriminator(),
                member.getId(),
                member.getUser().getEffectiveAvatarUrl() + "?size=1024",
                member.hasTimeJoined() ? TimeFormat.DATE_TIME_SHORT.atInstant(member.getTimeJoined().toInstant()).toString() : "-",
                TimeFormat.DATE_TIME_SHORT.atInstant(member.getTimeCreated().toInstant()).toString()
        };

        return EmbedFactory.getEmbedDefault(this, getString("template", argsArray)).
                setThumbnail(member.getUser().getEffectiveAvatarUrl());
    }

}
