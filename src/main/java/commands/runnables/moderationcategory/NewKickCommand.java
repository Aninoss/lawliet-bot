package commands.runnables.moderationcategory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import commands.listeners.CommandProperties;
import commands.Category;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.MemberCacheController;
import core.TextManager;
import core.mention.MentionList;
import core.mention.MentionValue;
import core.utils.MentionUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "newkick",
        botGuildPermissions = Permission.KICK_MEMBERS,
        userGuildPermissions = Permission.KICK_MEMBERS,
        emoji = "\uD83D\uDEAA",
        executableWithoutArgs = false,
        patreonRequired = true,
        requiresFullMemberCache = true,
        aliases = { "newcomerskick", "newcomerkick", "kicknew", "kicknewcomer", "kicknewcomers" }
)
public class NewKickCommand extends WarnCommand {

    private long minutes;

    public NewKickCommand(Locale locale, String prefix) {
        super(locale, prefix, true, false, false, false);
    }

    @Override
    protected boolean setUserListAndReason(GuildMessageReceivedEvent event, String args) throws Throwable {
        MentionValue<Long> mention = MentionUtil.getTimeMinutes(args);
        minutes = mention.getValue();
        if (minutes <= 0) {
            drawMessageNew(EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.MODERATION, "newkick_notime")))
                    .exceptionally(ExceptionLogger.get());
            return false;
        }

        return super.setUserListAndReason(event, mention.getFilteredArgs());
    }

    @Override
    protected MentionList<User> getUserList(Message message, String args) {
        List<User> userList = MemberCacheController.getInstance().loadMembersFull(message.getGuild()).join().stream()
                .filter(m -> m.hasTimeJoined() && m.getTimeJoined().toInstant().isAfter(Instant.now().minus(Duration.ofMinutes(minutes))))
                .map(Member::getUser)
                .collect(Collectors.toList());

        return new MentionList<>(args, userList);
    }

    @Override
    protected void process(Guild guild, User target, String reason) {
        guild.kick(target.getId(), reason)
                .submit()
                .exceptionally(e -> {
                    guild.kick(target.getId()).queue();
                    return null;
                });
    }

    @Override
    protected EmbedBuilder getNoMentionEmbed() {
        return EmbedFactory.getEmbedError(this, TextManager.getString(getLocale(), Category.MODERATION, "newkick_nomention"));
    }

}
