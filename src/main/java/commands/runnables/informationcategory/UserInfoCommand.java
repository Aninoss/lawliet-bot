package commands.runnables.informationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.MemberAccountAbstract;
import constants.AssetIds;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.util.Locale;

@CommandProperties(
        trigger = "userinfo",
        emoji = "\uD83D\uDC81",
        executableWithoutArgs = true,
        requiresFullMemberCache = true,
        aliases = { "userinfos", "userstat", "userstats", "accountinfo", "whois", "memberinfo", "user", "member" }
)
public class UserInfoCommand extends MemberAccountAbstract {

    public UserInfoCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false, true);
    }

    @Override
    protected EmbedBuilder processUser(CommandEvent event, User user, boolean userIsAuthor, String args) throws Throwable {
        Member member = event.getGuild().getMemberById(user.getIdLong());
        String[] type = getString("type").split("\n");
        int typeN = 0;
        if (!user.isBot()) {
            typeN = 1;
            if (event.getGuild().getOwnerIdLong() == user.getIdLong()) typeN = 2;
            if (user.getIdLong() == AssetIds.OWNER_USER_ID) typeN = 3;
        }
        String avatarUrl = member != null ? member.getEffectiveAvatarUrl() : user.getEffectiveAvatarUrl();

        String[] argsArray = {
                StringUtil.escapeMarkdown(user.getDiscriminator().equals("0000") ? user.getName() : user.getAsTag()),
                user.getGlobalName() != null ? StringUtil.escapeMarkdown(user.getGlobalName()) : "-",
                type[typeN],
                TextManager.getString(getLocale(), TextManager.GENERAL, "noyes", member != null),
                member != null && member.getNickname() != null ? StringUtil.escapeMarkdown(member.getNickname()) : "-",
                user.getId(),
                avatarUrl + "?size=1024",
                member != null && member.hasTimeJoined() ? TimeFormat.DATE_TIME_SHORT.atInstant(member.getTimeJoined().toInstant()).toString() : "-",
                TimeFormat.DATE_TIME_SHORT.atInstant(user.getTimeCreated().toInstant()).toString(),
        };

        return EmbedFactory.getEmbedDefault(this, getString("template", argsArray)).
                setThumbnail(avatarUrl);
    }

}
